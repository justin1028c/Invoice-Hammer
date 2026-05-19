package com.fordham.toolbelt.pdf

import com.fordham.toolbelt.domain.model.InvoiceData
import com.fordham.toolbelt.domain.repository.InvoiceEngine
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
class IosInvoiceEngine : InvoiceEngine {

    override fun generatePdf(data: InvoiceData): String? {
        val fileName = "${if (data.isEstimate) "Estimate" else "Invoice"}_${data.invoiceId}.pdf"
        // Stage into <Documents>/InvoiceHammer/Invoices/ so the file is born at the
        // canonical user-visible path that IosDocumentExporter will later confirm.
        // (UIActivityViewController shares directly from this sandbox path, so we
        // do not need to bounce through caches first.)
        val docsRoot = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        ).first() as String
        val fileManager = NSFileManager.defaultManager
        val invoicesDir = "$docsRoot/InvoiceHammer/Invoices"
        if (!fileManager.fileExistsAtPath(invoicesDir)) {
            fileManager.createDirectoryAtPath(
                invoicesDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }
        val filePath = "$invoicesDir/$fileName"

        val renderer = UIGraphicsPDFRenderer(bounds = CGRectMake(0.0, 0.0, 595.0, 842.0), format = UIGraphicsPDFRendererFormat())
        
        val pdfData = renderer.PDFDataWithActions { context ->
            // --- PAGE 1: BILLING SUMMARY ---
            context!!.beginPage()
            val cgContext = UIGraphicsGetCurrentContext()
            CGContextSetInterpolationQuality(cgContext, kCGInterpolationHigh)
            
            // Color Tokens
            val orangeColor = UIColor.colorWithRed(252.0 / 255.0, 102.0 / 255.0, 0.0 / 255.0, 1.0)
            val charcoalColor = UIColor.colorWithRed(30.0 / 255.0, 30.0 / 255.0, 30.0 / 255.0, 1.0)
            val grayColor = UIColor.colorWithRed(100.0 / 255.0, 100.0 / 255.0, 100.0 / 255.0, 1.0)
            val lightGrayColor = UIColor.colorWithRed(245.0 / 255.0, 245.0 / 255.0, 245.0 / 255.0, 1.0)
            
            // 1. Draw Business Details (Top Left)
            val bizName = (data.settings.businessName.ifBlank { "INVOICE HAMMER" }).uppercase()
            val bizAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(22.0),
                NSForegroundColorAttributeName to orangeColor
            )
            (bizName as NSString).drawAtPoint(CGPointMake(50.0, 40.0), withAttributes = bizAttributes as Map<Any?, *>)
            
            val slogan = data.settings.businessSlogan.ifBlank { "Professional Field Services" }
            val sloganAttributes = mapOf(
                NSFontAttributeName to UIFont.italicSystemFontOfSize(9.0),
                NSForegroundColorAttributeName to grayColor
            )
            (slogan as NSString).drawAtPoint(CGPointMake(50.0, 68.0), withAttributes = sloganAttributes as Map<Any?, *>)
            
            val contactAttributes = mapOf(
                NSFontAttributeName to UIFont.systemFontOfSize(10.0),
                NSForegroundColorAttributeName to charcoalColor
            )
            var contactOffset = 82.0
            if (data.settings.businessPhone.isNotBlank()) {
                ("P: ${data.settings.businessPhone}" as NSString).drawAtPoint(CGPointMake(50.0, contactOffset), withAttributes = contactAttributes as Map<Any?, *>)
                contactOffset += 13.0
            }
            if (data.settings.businessEmail.isNotBlank()) {
                ("E: ${data.settings.businessEmail}" as NSString).drawAtPoint(CGPointMake(50.0, contactOffset), withAttributes = contactAttributes as Map<Any?, *>)
                contactOffset += 13.0
            }
            if (data.settings.businessAddress.isNotBlank()) {
                ("A: ${data.settings.businessAddress}" as NSString).drawAtPoint(CGPointMake(50.0, contactOffset), withAttributes = contactAttributes as Map<Any?, *>)
            }
            
            // 2. Draw Logo (Top Right)
            val logoImage = data.logoUriString?.let { UIImage.imageWithContentsOfFile(it) }
                ?: data.settings.logoUri?.let { UIImage.imageWithContentsOfFile(it) }
            if (logoImage != null) {
                logoImage.drawInRect(CGRectMake(465.0, 40.0, 80.0, 80.0))
                // Draw a thin charcoal border around logo
                CGContextSetStrokeColorWithColor(cgContext, charcoalColor.CGColor)
                CGContextSetLineWidth(cgContext, 1.0)
                CGContextStrokeRect(cgContext, CGRectMake(465.0, 40.0, 80.0, 80.0))
            }
            
            // Divider line
            CGContextSetStrokeColorWithColor(cgContext, orangeColor.CGColor)
            CGContextSetLineWidth(cgContext, 3.0)
            CGContextBeginPath(cgContext)
            CGContextMoveToPoint(cgContext, 50.0, 145.0)
            CGContextAddLineToPoint(cgContext, 545.0, 145.0)
            CGContextStrokePath(cgContext)
            
            // 3. Client & Metadata Split Boxes (Y = 160.0)
            val sectionHeaderAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(10.0),
                NSForegroundColorAttributeName to orangeColor
            )
            val boldBodyAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(11.0),
                NSForegroundColorAttributeName to charcoalColor
            )
            val normalBodyAttributes = mapOf(
                NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                NSForegroundColorAttributeName to grayColor
            )
            
            // Billed To (Left Column)
            ("BILLED TO:" as NSString).drawAtPoint(CGPointMake(50.0, 165.0), withAttributes = sectionHeaderAttributes as Map<Any?, *>)
            (data.clientName.uppercase() as NSString).drawAtPoint(CGPointMake(50.0, 180.0), withAttributes = boldBodyAttributes as Map<Any?, *>)
            (data.clientAddress.ifBlank { "No Job Address Provided" } as NSString).drawAtPoint(CGPointMake(50.0, 193.0), withAttributes = normalBodyAttributes as Map<Any?, *>)
            
            // Document Meta (Right Column)
            ("DOCUMENT DETAILS:" as NSString).drawAtPoint(CGPointMake(330.0, 165.0), withAttributes = sectionHeaderAttributes as Map<Any?, *>)
            val docType = if (data.isEstimate) "ESTIMATE" else "INVOICE"
            ("$docType #${data.invoiceId.take(8).uppercase()}" as NSString).drawAtPoint(CGPointMake(330.0, 180.0), withAttributes = boldBodyAttributes as Map<Any?, *>)
            ("DATE: ${data.date}" as NSString).drawAtPoint(CGPointMake(330.0, 193.0), withAttributes = normalBodyAttributes as Map<Any?, *>)
            
            // 4. Line Items Table (Y = 230.0)
            var currentY = 230.0
            
            // Table Header
            CGContextSetFillColorWithColor(cgContext, charcoalColor.CGColor)
            CGContextFillRect(cgContext, CGRectMake(50.0, currentY, 495.0, 20.0))
            
            val tableHeaderAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(9.0),
                NSForegroundColorAttributeName to UIColor.whiteColor
            )
            ("DESCRIPTION" as NSString).drawAtPoint(CGPointMake(60.0, currentY + 4.0), withAttributes = tableHeaderAttributes as Map<Any?, *>)
            ("CATEGORY" as NSString).drawAtPoint(CGPointMake(320.0, currentY + 4.0), withAttributes = tableHeaderAttributes as Map<Any?, *>)
            ("TOTAL" as NSString).drawAtPoint(CGPointMake(495.0, currentY + 4.0), withAttributes = tableHeaderAttributes as Map<Any?, *>)
            
            currentY += 20.0
            
            // Table Rows
            data.items.forEachIndexed { idx, item ->
                if (idx % 2 == 1) {
                    CGContextSetFillColorWithColor(cgContext, lightGrayColor.CGColor)
                    CGContextFillRect(cgContext, CGRectMake(50.0, currentY, 495.0, 22.0))
                }
                
                val itemDescAttributes = mapOf(
                    NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                    NSForegroundColorAttributeName to charcoalColor
                )
                val itemCatAttributes = mapOf(
                    NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                    NSForegroundColorAttributeName to grayColor
                )
                val itemAmtAttributes = mapOf(
                    NSFontAttributeName to UIFont.boldSystemFontOfSize(9.0),
                    NSForegroundColorAttributeName to charcoalColor
                )
                
                val descText = if (item.description.length > 40) item.description.take(37) + "..." else item.description
                (descText as NSString).drawAtPoint(CGPointMake(60.0, currentY + 5.0), withAttributes = itemDescAttributes as Map<Any?, *>)
                (item.category.uppercase() as NSString).drawAtPoint(CGPointMake(320.0, currentY + 5.0), withAttributes = itemCatAttributes as Map<Any?, *>)
                
                val amtText = NSString.stringWithFormat("$%.2f", item.amount)
                (amtText as NSString).drawAtPoint(CGPointMake(495.0, currentY + 5.0), withAttributes = itemAmtAttributes as Map<Any?, *>)
                
                // Thin row divider line
                CGContextSetStrokeColorWithColor(cgContext, UIColor.colorWithRed(220.0/255.0, 220.0/255.0, 220.0/255.0, 1.0).CGColor)
                CGContextSetLineWidth(cgContext, 0.5)
                CGContextBeginPath(cgContext)
                CGContextMoveToPoint(cgContext, 50.0, currentY + 22.0)
                CGContextAddLineToPoint(cgContext, 545.0, currentY + 22.0)
                CGContextStrokePath(cgContext)
                
                currentY += 22.0
            }
            
            // Calculations Block
            currentY += 15.0
            val subtotal = data.items.sumOf { it.amount }
            val taxAmount = subtotal * (data.taxRate / 100.0)
            val totalAmount = subtotal + taxAmount - data.deposit
            
            val calcLabelAttributes = mapOf(
                NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                NSForegroundColorAttributeName to grayColor
            )
            
            ("Subtotal:" as NSString).drawAtPoint(CGPointMake(330.0, currentY), withAttributes = calcLabelAttributes as Map<Any?, *>)
            (NSString.stringWithFormat("$%.2f", subtotal) as NSString).drawAtPoint(CGPointMake(495.0, currentY), withAttributes = calcLabelAttributes as Map<Any?, *>)
            currentY += 14.0
            
            ("Tax (${data.taxRate}%):" as NSString).drawAtPoint(CGPointMake(330.0, currentY), withAttributes = calcLabelAttributes as Map<Any?, *>)
            (NSString.stringWithFormat("$%.2f", taxAmount) as NSString).drawAtPoint(CGPointMake(495.0, currentY), withAttributes = calcLabelAttributes as Map<Any?, *>)
            currentY += 14.0
            
            if (data.deposit > 0.0) {
                ("Deposit Collected:" as NSString).drawAtPoint(CGPointMake(330.0, currentY), withAttributes = calcLabelAttributes as Map<Any?, *>)
                (NSString.stringWithFormat("-$%.2f", data.deposit) as NSString).drawAtPoint(CGPointMake(495.0, currentY), withAttributes = calcLabelAttributes as Map<Any?, *>)
                currentY += 14.0
            }
            
            // Grand Total Box
            currentY += 5.0
            CGContextSetFillColorWithColor(cgContext, orangeColor.CGColor)
            CGContextFillRect(cgContext, CGRectMake(330.0, currentY, 215.0, 26.0))
            
            val totalBoxLabelAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(10.0),
                NSForegroundColorAttributeName to UIColor.whiteColor
            )
            val totalBoxValAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(12.0),
                NSForegroundColorAttributeName to UIColor.whiteColor
            )
            ("TOTAL DUE:" as NSString).drawAtPoint(CGPointMake(340.0, currentY + 6.0), withAttributes = totalBoxLabelAttributes as Map<Any?, *>)
            
            val grandTotalText = NSString.stringWithFormat("$%.2f", totalAmount)
            (grandTotalText as NSString).drawAtPoint(CGPointMake(490.0, currentY + 5.0), withAttributes = totalBoxValAttributes as Map<Any?, *>)
            
            // Footer text
            val footerAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(9.0),
                NSForegroundColorAttributeName to grayColor
            )
            ("THANK YOU FOR YOUR BUSINESS!" as NSString).drawAtPoint(CGPointMake(195.0, 790.0), withAttributes = footerAttributes as Map<Any?, *>)
            
            // --- PAGE 2+: DYNAMIC JOB SITE GALLERY ---
            if (data.photoUris.isNotEmpty()) {
                var photoIdx = 0
                var pageNum = 2
                
                while (photoIdx < data.photoUris.size) {
                    context.beginPage()
                    CGContextSetInterpolationQuality(cgContext, kCGInterpolationHigh)
                    
                    // Reset drawing color for header
                    val galleryTitleAttributes = mapOf(
                        NSFontAttributeName to UIFont.boldSystemFontOfSize(18.0),
                        NSForegroundColorAttributeName to orangeColor
                    )
                    ("JOB SITE GALLERY - PAGE ${pageNum - 1}" as NSString).drawAtPoint(CGPointMake(50.0, 45.0), withAttributes = galleryTitleAttributes as Map<Any?, *>)
                    
                    CGContextSetStrokeColorWithColor(cgContext, charcoalColor.CGColor)
                    CGContextSetLineWidth(cgContext, 2.0)
                    CGContextBeginPath(cgContext)
                    CGContextMoveToPoint(cgContext, 50.0, 72.0)
                    CGContextAddLineToPoint(cgContext, 545.0, 72.0)
                    CGContextStrokePath(cgContext)
                    
                    val photosOnThisPage = minOf(8, data.photoUris.size - photoIdx)
                    for (i in 0 until photosOnThisPage) {
                        val currentUri = data.photoUris[photoIdx + i]
                        val row = i / 2
                        val col = i % 2
                        
                        val x = 50.0 + col * 255.0
                        val y = 90.0 + row * 165.0
                        
                        val photoImage = UIImage.imageWithContentsOfFile(currentUri)
                        if (photoImage != null) {
                            // Aspect-fill crop to the 240×130 cell BEFORE drawing. Without this,
                            // UIImage.drawInRect stretches a 4:3 camera photo into a 1.85:1 cell
                            // which (combined with the heavy downscale) showed up as the smeared
                            // / blurred look users were seeing.
                            CGContextSetInterpolationQuality(cgContext, kCGInterpolationHigh)
                            drawAspectFillImage(photoImage, CGRectMake(x, y, 240.0, 130.0))

                            // Thin border
                            CGContextSetStrokeColorWithColor(cgContext, charcoalColor.CGColor)
                            CGContextSetLineWidth(cgContext, 1.5)
                            CGContextStrokeRect(cgContext, CGRectMake(x, y, 240.0, 130.0))
                            
                            // Caption Under photo
                            if (col == 0) {
                                val captionAttributes = mapOf(
                                    NSFontAttributeName to UIFont.boldSystemFontOfSize(8.0),
                                    NSForegroundColorAttributeName to charcoalColor
                                )
                                ("[ BEFORE WORK ]" as NSString).drawAtPoint(CGPointMake(x + 5.0, y + 138.0), withAttributes = captionAttributes as Map<Any?, *>)
                            } else {
                                val captionAttributes = mapOf(
                                    NSFontAttributeName to UIFont.boldSystemFontOfSize(8.0),
                                    NSForegroundColorAttributeName to orangeColor
                                )
                                ("[ AFTER WORK ]" as NSString).drawAtPoint(CGPointMake(x + 5.0, y + 138.0), withAttributes = captionAttributes as Map<Any?, *>)
                            }
                        }
                    }
                    
                    photoIdx += photosOnThisPage
                    pageNum++
                }
            }
        }

        return if (pdfData.writeToFile(filePath, atomically = true)) {
            filePath
        } else {
            null
        }
    }

    /**
     * Aspect-fill (centre-crop) [image] into [rect]. UIImage.drawInRect stretches by
     * default; clipping to [rect] and then drawing the image at the rect aspect ratio
     * recreates an aspect-fill semantic, which matches what the Android engine does
     * and matches what users expect from a "photo on a page" layout.
     *
     * Uses kCGInterpolationHigh implicitly because the surrounding PDF context sets
     * it; the centre-crop keeps anisotropic stretching out of the picture so the
     * remaining downscale renders crisply.
     */
    private fun drawAspectFillImage(image: UIImage, rect: CValue<CGRect>) {
        val (imgW, imgH) = image.size.useContents { width to height }
        data class Box(val x: Double, val y: Double, val w: Double, val h: Double)
        val r = rect.useContents { Box(origin.x, origin.y, size.width, size.height) }

        if (imgW <= 0.0 || imgH <= 0.0 || r.w <= 0.0 || r.h <= 0.0) return

        val cgContext = UIGraphicsGetCurrentContext()
        CGContextSaveGState(cgContext)
        CGContextAddRect(cgContext, rect)
        CGContextClip(cgContext)

        // Scale up so the SHORT axis of the image covers the cell (aspect-fill).
        val scale = maxOf(r.w / imgW, r.h / imgH)
        val drawW = imgW * scale
        val drawH = imgH * scale
        val drawX = r.x + (r.w - drawW) / 2.0
        val drawY = r.y + (r.h - drawH) / 2.0

        image.drawInRect(CGRectMake(drawX, drawY, drawW, drawH))

        CGContextRestoreGState(cgContext)
    }
}
