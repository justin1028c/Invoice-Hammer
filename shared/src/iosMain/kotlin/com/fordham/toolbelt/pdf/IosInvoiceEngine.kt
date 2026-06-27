package com.fordham.toolbelt.pdf

import com.fordham.toolbelt.domain.model.InvoiceData
import com.fordham.toolbelt.domain.repository.InvoiceEngine
import com.fordham.toolbelt.util.UserFacingCopy
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
class IosInvoiceEngine : InvoiceEngine {

    override fun generatePdf(data: InvoiceData): String? {
        val pdf = UserFacingCopy.Pdf
        val filePrefix = if (data.isEstimate) pdf.estimateFilePrefix() else pdf.invoiceFilePrefix()
        val fileName = "${filePrefix}_${data.invoiceId}.pdf"
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
            
            // Core HSL-aligned Premium Palette
            val orangeColor = UIColor.colorWithRed(252.0 / 255.0, 102.0 / 255.0, 0.0 / 255.0, 1.0)
            val charcoalColor = UIColor.colorWithRed(28.0 / 255.0, 28.0 / 255.0, 30.0 / 255.0, 1.0)
            val mutedCharcoal = UIColor.colorWithRed(44.0 / 255.0, 44.0 / 255.0, 46.0 / 255.0, 1.0)
            val grayColor = UIColor.colorWithRed(100.0 / 255.0, 100.0 / 255.0, 105.0 / 255.0, 1.0)
            val lightGrayColor = UIColor.colorWithRed(250.0 / 255.0, 250.0 / 255.0, 252.0 / 255.0, 1.0)
            val borderGrayColor = UIColor.colorWithRed(230.0 / 255.0, 230.0 / 255.0, 235.0 / 255.0, 1.0)
            val greenColor = UIColor.colorWithRed(30.0 / 255.0, 142.0 / 255.0, 62.0 / 255.0, 1.0)
            
            // 1. Draw Business Details (Top Left)
            val bizName = (data.settings.businessName.ifBlank { pdf.defaultBusinessName() }).uppercase()
            val bizAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(24.0),
                NSForegroundColorAttributeName to orangeColor
            )
            (bizName as NSString).drawAtPoint(CGPointMake(50.0, 40.0), withAttributes = bizAttributes as Map<Any?, *>)
            
            val slogan = data.settings.businessSlogan.ifBlank { pdf.defaultSlogan() }
            val sloganAttributes = mapOf(
                NSFontAttributeName to UIFont.italicSystemFontOfSize(10.0),
                NSForegroundColorAttributeName to grayColor
            )
            (slogan as NSString).drawAtPoint(CGPointMake(50.0, 68.0), withAttributes = sloganAttributes as Map<Any?, *>)
            
            val contactAttributes = mapOf(
                NSFontAttributeName to UIFont.systemFontOfSize(9.5),
                NSForegroundColorAttributeName to mutedCharcoal
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
                IosInvoicePdfDrawing.drawLogoContained(logoImage, slotX = 465.0, slotY = 40.0, slotW = 80.0, slotH = 80.0)
                
                val borderPath = UIBezierPath.bezierPathWithRoundedRect(CGRectMake(465.0, 40.0, 80.0, 80.0), cornerRadius = 6.0)
                borderGrayColor.setStroke()
                borderPath.lineWidth = 1.0
                borderPath.stroke()
            }
            
            // Divider line
            CGContextSetStrokeColorWithColor(cgContext, orangeColor.CGColor)
            CGContextSetLineWidth(cgContext, 2.5)
            CGContextBeginPath(cgContext)
            CGContextMoveToPoint(cgContext, 50.0, 142.0)
            CGContextAddLineToPoint(cgContext, 545.0, 142.0)
            CGContextStrokePath(cgContext)
            
            // 3. Client & Metadata Split Rounded Cards (Y = 155.0)
            val cardTop = 155.0
            val cardHeight = 75.0
            
            // Client Card (Left Column)
            val leftCardPath = UIBezierPath.bezierPathWithRoundedRect(CGRectMake(50.0, cardTop, 235.0, cardHeight), cornerRadius = 8.0)
            lightGrayColor.setFill()
            leftCardPath.fill()
            borderGrayColor.setStroke()
            leftCardPath.lineWidth = 1.0
            leftCardPath.stroke()
            
            val sectionHeaderAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(9.0),
                NSForegroundColorAttributeName to orangeColor
            )
            val normalBodyAttributes = mapOf(
                NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                NSForegroundColorAttributeName to grayColor
            )
            val boldBodyAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(9.0),
                NSForegroundColorAttributeName to charcoalColor
            )
            
            (pdf.billedTo() as NSString).drawAtPoint(CGPointMake(65.0, cardTop + 10.0), withAttributes = sectionHeaderAttributes as Map<Any?, *>)

            val nameText = data.clientName.uppercase()
            val maxNameWidth = 210.0
            var nameFontSize = 12.0
            var nameAttributes = mapOf<Any?, Any?>(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(nameFontSize),
                NSForegroundColorAttributeName to charcoalColor
            )
            while ((nameText as NSString).sizeWithAttributes(nameAttributes as Map<Any?, *>).useContents { width } > maxNameWidth && nameFontSize > 8.0) {
                nameFontSize -= 0.5
                nameAttributes = mapOf<Any?, Any?>(
                    NSFontAttributeName to UIFont.boldSystemFontOfSize(nameFontSize),
                    NSForegroundColorAttributeName to charcoalColor
                )
            }
            val finalNameNSString = if ((nameText as NSString).sizeWithAttributes(nameAttributes as Map<Any?, *>).useContents { width } > maxNameWidth) {
                var truncated = nameText
                var truncatedNSString = "$truncated..." as NSString
                while (truncatedNSString.sizeWithAttributes(nameAttributes as Map<Any?, *>).useContents { width } > maxNameWidth && truncated.isNotEmpty()) {
                    truncated = truncated.dropLast(1)
                    truncatedNSString = "$truncated..." as NSString
                }
                truncatedNSString
            } else {
                nameText as NSString
            }
            finalNameNSString.drawAtPoint(CGPointMake(65.0, cardTop + 26.0), withAttributes = nameAttributes as Map<Any?, *>)
            
            val clientAddressText = data.clientAddress.ifBlank { pdf.noJobAddress() }
            val truncatedClientAddress = if (clientAddressText.length > 38) clientAddressText.take(35) + "..." else clientAddressText
            (truncatedClientAddress as NSString).drawAtPoint(CGPointMake(65.0, cardTop + 44.0), withAttributes = normalBodyAttributes as Map<Any?, *>)
            
            // Details Card (Right Column)
            val rightCardPath = UIBezierPath.bezierPathWithRoundedRect(CGRectMake(310.0, cardTop, 235.0, cardHeight), cornerRadius = 8.0)
            lightGrayColor.setFill()
            rightCardPath.fill()
            borderGrayColor.setStroke()
            rightCardPath.lineWidth = 1.0
            rightCardPath.stroke()
            
            (pdf.documentDetails() as NSString).drawAtPoint(CGPointMake(325.0, cardTop + 10.0), withAttributes = sectionHeaderAttributes as Map<Any?, *>)
            (pdf.docNumber(data.isEstimate, data.invoiceId) as NSString).drawAtPoint(CGPointMake(325.0, cardTop + 26.0), withAttributes = boldBodyAttributes as Map<Any?, *>)
            (pdf.datePrefix(data.date) as NSString).drawAtPoint(CGPointMake(325.0, cardTop + 44.0), withAttributes = normalBodyAttributes as Map<Any?, *>)
            
            // 4. Status Badge Pill inside details card
            val statusText: String
            val badgeBgColor: UIColor
            val badgeTextColor: UIColor
            
            if (data.isEstimate) {
                statusText = pdf.statusEstimate()
                badgeBgColor = UIColor.colorWithRed(232.0/255.0, 240.0/255.0, 254.0/255.0, 1.0)
                badgeTextColor = UIColor.colorWithRed(26.0/255.0, 115.0/255.0, 232.0/255.0, 1.0)
            } else {
                val subtotal = data.items.sumOf { it.amount }
                val taxAmount = subtotal * (data.taxRate / 100.0)
                if (data.deposit >= (subtotal + taxAmount)) {
                    statusText = pdf.statusPaid()
                    badgeBgColor = UIColor.colorWithRed(230.0/255.0, 244.0/255.0, 234.0/255.0, 1.0)
                    badgeTextColor = UIColor.colorWithRed(30.0/255.0, 142.0/255.0, 62.0/255.0, 1.0)
                } else if (data.deposit > 0.0) {
                    statusText = pdf.statusPartial()
                    badgeBgColor = UIColor.colorWithRed(254.0/255.0, 243.0/255.0, 224.0/255.0, 1.0)
                    badgeTextColor = UIColor.colorWithRed(230.0/255.0, 81.0/255.0, 0.0/255.0, 1.0)
                } else {
                    statusText = pdf.statusDue()
                    badgeBgColor = UIColor.colorWithRed(253.0/255.0, 236.0/255.0, 236.0/255.0, 1.0)
                    badgeTextColor = UIColor.colorWithRed(217.0/255.0, 48.0/255.0, 37.0/255.0, 1.0)
                }
            }
            
            val statusBadgePath = UIBezierPath.bezierPathWithRoundedRect(CGRectMake(465.0, cardTop + 10.0, 70.0, 14.0), cornerRadius = 7.0)
            badgeBgColor.setFill()
            statusBadgePath.fill()
            
            val badgeStyle = NSMutableParagraphStyle().apply {
                alignment = NSTextAlignmentCenter
            }
            val badgeAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(7.5),
                NSForegroundColorAttributeName to badgeTextColor,
                NSParagraphStyleAttributeName to badgeStyle
            )
            (statusText as NSString).drawInRect(CGRectMake(465.0, cardTop + 11.5, 70.0, 14.0), withAttributes = badgeAttributes as Map<Any?, *>)
            
            // 5. Line Items Table (Y = 245.0)
            var currentY = 245.0
            
            // Premium Table Header background
            CGContextSetFillColorWithColor(cgContext, UIColor.colorWithRed(240.0/255.0, 240.0/255.0, 245.0/255.0, 1.0).CGColor)
            CGContextFillRect(cgContext, CGRectMake(50.0, currentY, 495.0, 22.0))
            
            val rightAlignStyle = NSMutableParagraphStyle().apply {
                alignment = NSTextAlignmentRight
            }
            val tableHeaderAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(8.5),
                NSForegroundColorAttributeName to charcoalColor
            )
            val tableHeaderRightAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(8.5),
                NSForegroundColorAttributeName to charcoalColor,
                NSParagraphStyleAttributeName to rightAlignStyle
            )
            
            (pdf.description() as NSString).drawAtPoint(CGPointMake(60.0, currentY + 4.0), withAttributes = tableHeaderAttributes as Map<Any?, *>)
            (pdf.qty() as NSString).drawInRect(CGRectMake(300.0, currentY + 4.0, 60.0, 15.0), withAttributes = tableHeaderRightAttributes as Map<Any?, *>)
            (pdf.unitPrice() as NSString).drawInRect(CGRectMake(370.0, currentY + 4.0, 80.0, 15.0), withAttributes = tableHeaderRightAttributes as Map<Any?, *>)
            (pdf.total() as NSString).drawInRect(CGRectMake(460.0, currentY + 4.0, 75.0, 15.0), withAttributes = tableHeaderRightAttributes as Map<Any?, *>)
            
            currentY += 22.0
            
            // Table Rows
            data.items.forEachIndexed { idx, item ->
                if (idx % 2 == 1) {
                    CGContextSetFillColorWithColor(cgContext, lightGrayColor.CGColor)
                    CGContextFillRect(cgContext, CGRectMake(50.0, currentY, 495.0, 24.0))
                }
                
                // Draw a very thin row outline or separator
                CGContextSetStrokeColorWithColor(cgContext, borderGrayColor.CGColor)
                CGContextSetLineWidth(cgContext, 0.5)
                CGContextBeginPath(cgContext)
                CGContextMoveToPoint(cgContext, 50.0, currentY + 24.0)
                CGContextAddLineToPoint(cgContext, 545.0, currentY + 24.0)
                CGContextStrokePath(cgContext)
                
                val itemDescAttributes = mapOf(
                    NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                    NSForegroundColorAttributeName to charcoalColor
                )
                val itemMutedRightAttributes = mapOf(
                    NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                    NSForegroundColorAttributeName to grayColor,
                    NSParagraphStyleAttributeName to rightAlignStyle
                )
                val itemBoldRightAttributes = mapOf(
                    NSFontAttributeName to UIFont.boldSystemFontOfSize(9.0),
                    NSForegroundColorAttributeName to charcoalColor,
                    NSParagraphStyleAttributeName to rightAlignStyle
                )
                
                val descText = if (item.description.length > 48) item.description.take(45) + "..." else item.description
                (descText as NSString).drawAtPoint(CGPointMake(60.0, currentY + 5.0), withAttributes = itemDescAttributes as Map<Any?, *>)
                
                val qtyVal = item.quantity ?: 1.0
                val qtyText = if (qtyVal % 1.0 == 0.0) qtyVal.toInt().toString() else NSString.stringWithFormat("%.2f", qtyVal)
                (qtyText as NSString).drawInRect(CGRectMake(300.0, currentY + 5.0, 60.0, 15.0), withAttributes = itemMutedRightAttributes as Map<Any?, *>)
                
                val unitPriceVal = item.unitPrice ?: item.amount
                val unitPriceText = NSString.stringWithFormat("$%.2f", unitPriceVal)
                (unitPriceText as NSString).drawInRect(CGRectMake(370.0, currentY + 5.0, 80.0, 15.0), withAttributes = itemMutedRightAttributes as Map<Any?, *>)
                
                val amtText = NSString.stringWithFormat("$%.2f", item.amount)
                (amtText as NSString).drawInRect(CGRectMake(460.0, currentY + 5.0, 75.0, 15.0), withAttributes = itemBoldRightAttributes as Map<Any?, *>)
                
                currentY += 24.0
            }
            
            // Calculations Block
            currentY += 15.0
            val subtotal = data.items.sumOf { it.amount }
            val taxAmount = subtotal * (data.taxRate / 100.0)
            val totalAmount = subtotal + taxAmount - data.deposit
            
            val calcBoxHeight = if (data.deposit > 0.0) 96.0 else 80.0
            val calcBoxPath = UIBezierPath.bezierPathWithRoundedRect(CGRectMake(325.0, currentY, 220.0, calcBoxHeight), cornerRadius = 8.0)
            
            lightGrayColor.setFill()
            calcBoxPath.fill()
            borderGrayColor.setStroke()
            calcBoxPath.lineWidth = 1.0
            calcBoxPath.stroke()
            
            var calcY = currentY + 8.0
            val calcLabelAttributes = mapOf(
                NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                NSForegroundColorAttributeName to grayColor
            )
            val calcValRightAttributes = mapOf(
                NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                NSForegroundColorAttributeName to grayColor,
                NSParagraphStyleAttributeName to rightAlignStyle
            )
            val calcGreenValRightAttributes = mapOf(
                NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                NSForegroundColorAttributeName to greenColor,
                NSParagraphStyleAttributeName to rightAlignStyle
            )
            
            (pdf.subtotal() as NSString).drawAtPoint(CGPointMake(340.0, calcY), withAttributes = calcLabelAttributes as Map<Any?, *>)
            (NSString.stringWithFormat("$%.2f", subtotal) as NSString).drawInRect(CGRectMake(430.0, calcY, 100.0, 15.0), withAttributes = calcValRightAttributes as Map<Any?, *>)
            calcY += 16.0
            
            (pdf.tax(data.taxRate) as NSString).drawAtPoint(CGPointMake(340.0, calcY), withAttributes = calcLabelAttributes as Map<Any?, *>)
            (NSString.stringWithFormat("$%.2f", taxAmount) as NSString).drawInRect(CGRectMake(430.0, calcY, 100.0, 15.0), withAttributes = calcValRightAttributes as Map<Any?, *>)
            calcY += 16.0
            
            if (data.deposit > 0.0) {
                (pdf.depositPaid() as NSString).drawAtPoint(CGPointMake(340.0, calcY), withAttributes = calcLabelAttributes as Map<Any?, *>)
                (NSString.stringWithFormat("-$%.2f", data.deposit) as NSString).drawInRect(CGRectMake(430.0, calcY, 100.0, 15.0), withAttributes = calcGreenValRightAttributes as Map<Any?, *>)
                calcY += 16.0
            }
            
            // Divider line
            CGContextSetStrokeColorWithColor(cgContext, borderGrayColor.CGColor)
            CGContextSetLineWidth(cgContext, 1.0)
            CGContextBeginPath(cgContext)
            CGContextMoveToPoint(cgContext, 325.0, calcY - 4.0)
            CGContextAddLineToPoint(cgContext, 545.0, calcY - 4.0)
            CGContextStrokePath(cgContext)
            
            val totalDueLabelAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(10.0),
                NSForegroundColorAttributeName to charcoalColor
            )
            val totalDueValRightAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(12.0),
                NSForegroundColorAttributeName to orangeColor,
                NSParagraphStyleAttributeName to rightAlignStyle
            )
            
            (pdf.totalDue() as NSString).drawAtPoint(CGPointMake(340.0, calcY + 2.0), withAttributes = totalDueLabelAttributes as Map<Any?, *>)
            
            val grandTotalText = NSString.stringWithFormat("$%.2f", totalAmount)
            (grandTotalText as NSString).drawInRect(CGRectMake(430.0, calcY + 1.0, 100.0, 18.0), withAttributes = totalDueValRightAttributes as Map<Any?, *>)
            
            // Footer text
            val footerAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(9.0),
                NSForegroundColorAttributeName to grayColor
            )
            val thankYouText = pdf.thankYou()
            val thankYouNSString = thankYouText as NSString
            val thankYouSize = thankYouNSString.sizeWithAttributes(footerAttributes as Map<Any?, *>)
            val thankYouWidth = thankYouSize.useContents { width }
            
            thankYouNSString.drawAtPoint(CGPointMake((595.0 - thankYouWidth) / 2.0, 790.0), withAttributes = footerAttributes as Map<Any?, *>)
            
            // --- PAGE 2+: DYNAMIC JOB SITE GALLERY ---
            val galleryRows = com.fordham.toolbelt.domain.model.buildJobPhotoGalleryRows(data.jobSitePhotos)
            if (galleryRows.isNotEmpty()) {
                var rowIdx = 0
                var pageNum = 2

                while (rowIdx < galleryRows.size) {
                    context.beginPage()
                    CGContextSetInterpolationQuality(cgContext, kCGInterpolationHigh)

                    val galleryTitleAttributes = mapOf(
                        NSFontAttributeName to UIFont.boldSystemFontOfSize(18.0),
                        NSForegroundColorAttributeName to orangeColor
                    )
                    (pdf.galleryPageTitle(pageNum - 1) as NSString).drawAtPoint(
                        CGPointMake(50.0, 45.0),
                        withAttributes = galleryTitleAttributes as Map<Any?, *>
                    )

                    CGContextSetStrokeColorWithColor(cgContext, charcoalColor.CGColor)
                    CGContextSetLineWidth(cgContext, 2.0)
                    CGContextBeginPath(cgContext)
                    CGContextMoveToPoint(cgContext, 50.0, 72.0)
                    CGContextAddLineToPoint(cgContext, 545.0, 72.0)
                    CGContextStrokePath(cgContext)

                    val rowsOnThisPage = minOf(4, galleryRows.size - rowIdx)
                    for (r in 0 until rowsOnThisPage) {
                        val galleryRow = galleryRows[rowIdx + r]
                        val y = 90.0 + r * 165.0

                        fun drawGalleryCell(uri: String, col: Int, isBefore: Boolean) {
                            val x = 50.0 + col * 255.0
                            val photoImage = UIImage.imageWithContentsOfFile(uri) ?: return
                            CGContextSetInterpolationQuality(cgContext, kCGInterpolationHigh)
                            IosInvoicePdfDrawing.drawAspectFillImage(photoImage, CGRectMake(x, y, 240.0, 130.0))
                            CGContextSetStrokeColorWithColor(cgContext, charcoalColor.CGColor)
                            CGContextSetLineWidth(cgContext, 1.5)
                            CGContextStrokeRect(cgContext, CGRectMake(x, y, 240.0, 130.0))
                            val captionColor = if (isBefore) charcoalColor else orangeColor
                            val captionAttributes = mapOf(
                                NSFontAttributeName to UIFont.boldSystemFontOfSize(8.0),
                                NSForegroundColorAttributeName to captionColor
                            )
                            val caption = if (isBefore) pdf.beforeWorkCaption() else pdf.afterWorkCaption()
                            (caption as NSString).drawAtPoint(
                                CGPointMake(x + 5.0, y + 138.0),
                                withAttributes = captionAttributes as Map<Any?, *>
                            )
                        }

                        galleryRow.beforeUri?.let { drawGalleryCell(it, col = 0, isBefore = true) }
                        galleryRow.afterUri?.let { drawGalleryCell(it, col = 1, isBefore = false) }
                    }

                    rowIdx += rowsOnThisPage
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
}
