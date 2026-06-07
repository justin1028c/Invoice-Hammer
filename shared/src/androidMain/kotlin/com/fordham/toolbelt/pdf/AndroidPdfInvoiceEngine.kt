package com.fordham.toolbelt.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.fordham.toolbelt.domain.model.InvoiceData
import com.fordham.toolbelt.util.SecurityManager
import java.io.File
import java.io.FileOutputStream

class AndroidPdfInvoiceEngine(
    private val context: Context,
    private val securityManager: SecurityManager
) : com.fordham.toolbelt.domain.repository.InvoiceEngine {
    
    override fun generatePdf(data: InvoiceData): String? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        
        // --- PAGE 1: BILLING SUMMARY ---
        val page1 = pdfDocument.startPage(pageInfo)
        val canvas1 = page1.canvas
        val paint = Paint().apply { 
            isAntiAlias = true 
            isFilterBitmap = true
            isDither = true
        }
        
        // Premium HSL-aligned Color Palette
        val orangeColor = Color.rgb(252, 102, 0)
        val charcoalColor = Color.rgb(28, 28, 30)
        val mutedCharcoal = Color.rgb(44, 44, 46)
        val grayColor = Color.rgb(100, 100, 105)
        val lightGrayColor = Color.rgb(250, 250, 252)
        val borderGrayColor = Color.rgb(230, 230, 235)
        
        // 1. Draw Business Details (Top Left)
        paint.color = orangeColor
        paint.textSize = 24f
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        val bizName = data.settings.businessName.ifBlank { "INVOICE HAMMER" }.uppercase()
        canvas1.drawText(bizName, 50f, 65f, paint)
        
        paint.color = grayColor
        paint.textSize = 10f
        paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        val slogan = data.settings.businessSlogan.ifBlank { "Professional Field Services" }
        canvas1.drawText(slogan, 50f, 82f, paint)
        
        paint.color = mutedCharcoal
        paint.textSize = 9.5f
        paint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        var contactOffset = 98f
        if (data.settings.businessPhone.isNotBlank()) {
            canvas1.drawText("P: ${data.settings.businessPhone}", 50f, contactOffset, paint)
            contactOffset += 13f
        }
        if (data.settings.businessEmail.isNotBlank()) {
            canvas1.drawText("E: ${data.settings.businessEmail}", 50f, contactOffset, paint)
            contactOffset += 13f
        }
        if (data.settings.businessAddress.isNotBlank()) {
            canvas1.drawText("A: ${data.settings.businessAddress}", 50f, contactOffset, paint)
        }
        
        // 2. Draw Logo (Top Right)
        val logoSlot = RectF(465f, 40f, 545f, 120f)
        val logoBitmap = AndroidPdfInvoiceBitmapUtils.decodeUriToBitmap(
            context,
            data.logoUriString ?: data.settings.logoUri,
            targetW = 320,
            targetH = 320
        )
        if (logoBitmap != null) {
            val logoReady = AndroidPdfInvoiceBitmapUtils.prepareLogoContained(logoBitmap, slotW = 80, slotH = 80)
            if (logoReady !== logoBitmap) {
                logoBitmap.recycle()
            }
            paint.isFilterBitmap = true
            paint.isAntiAlias = true
            canvas1.drawBitmap(logoReady, null, logoSlot, paint)
            logoReady.recycle()
            
            paint.color = borderGrayColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas1.drawRoundRect(logoSlot, 6f, 6f, paint)
            paint.style = Paint.Style.FILL
        }
        
        // Divider line
        paint.color = orangeColor
        paint.strokeWidth = 2.5f
        canvas1.drawLine(50f, 142f, 545f, 142f, paint)
        
        // 3. Client & Metadata Split Rounded Cards (Y = 155f)
        val cardTop = 155f
        val cardHeight = 75f
        
        // Client Card (Left Column)
        val leftCard = RectF(50f, cardTop, 285f, cardTop + cardHeight)
        paint.color = lightGrayColor
        paint.style = Paint.Style.FILL
        canvas1.drawRoundRect(leftCard, 8f, 8f, paint)
        paint.color = borderGrayColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas1.drawRoundRect(leftCard, 8f, 8f, paint)
        paint.style = Paint.Style.FILL
        
        paint.color = orangeColor
        paint.textSize = 9f
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        canvas1.drawText("BILLED TO", 65f, cardTop + 20f, paint)
        
        paint.color = charcoalColor
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        val nameText = data.clientName.uppercase()
        val maxNameWidth = 210f
        var currentTextSize = 12f
        paint.textSize = currentTextSize
        while (paint.measureText(nameText) > maxNameWidth && currentTextSize > 8f) {
            currentTextSize -= 0.5f
            paint.textSize = currentTextSize
        }
        val finalNameText = if (paint.measureText(nameText) > maxNameWidth) {
            var truncated = nameText
            while (paint.measureText(truncated + "...") > maxNameWidth && truncated.isNotEmpty()) {
                truncated = truncated.dropLast(1)
            }
            truncated + "..."
        } else {
            nameText
        }
        canvas1.drawText(finalNameText, 65f, cardTop + 38f, paint)
        
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        val clientAddressText = data.clientAddress.ifBlank { "No Job Address Provided" }
        // Simple one-line address drawing
        val truncatedClientAddress = if (clientAddressText.length > 38) clientAddressText.take(35) + "..." else clientAddressText
        canvas1.drawText(truncatedClientAddress, 65f, cardTop + 54f, paint)
        
        // Details Card (Right Column)
        val rightCard = RectF(310f, cardTop, 545f, cardTop + cardHeight)
        paint.color = lightGrayColor
        paint.style = Paint.Style.FILL
        canvas1.drawRoundRect(rightCard, 8f, 8f, paint)
        paint.color = borderGrayColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas1.drawRoundRect(rightCard, 8f, 8f, paint)
        paint.style = Paint.Style.FILL
        
        paint.color = orangeColor
        paint.textSize = 9f
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        canvas1.drawText("DOCUMENT DETAILS", 325f, cardTop + 20f, paint)
        
        paint.color = charcoalColor
        paint.textSize = 11f
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        val docType = if (data.isEstimate) "ESTIMATE" else "INVOICE"
        canvas1.drawText("$docType #${data.invoiceId.take(8).uppercase()}", 325f, cardTop + 38f, paint)
        
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        canvas1.drawText("DATE: ${data.date}", 325f, cardTop + 54f, paint)
        
        // 4. Status Badge Pill (Drawn inside the details card top-right)
        val statusText: String
        val badgeBgColor: Int
        val badgeTextColor: Int
        
        if (data.isEstimate) {
            statusText = "ESTIMATE"
            badgeBgColor = Color.rgb(232, 240, 254) // Soft blue
            badgeTextColor = Color.rgb(26, 115, 232) // Rich blue
        } else {
            val subtotal = data.items.sumOf { it.amount }
            val taxAmount = subtotal * (data.taxRate / 100.0)
            val totalAmount = subtotal + taxAmount - data.deposit
            if (data.deposit >= (subtotal + taxAmount)) {
                statusText = "PAID"
                badgeBgColor = Color.rgb(230, 244, 234) // Soft emerald
                badgeTextColor = Color.rgb(30, 142, 62) // Rich emerald
            } else if (data.deposit > 0.0) {
                statusText = "PARTIAL"
                badgeBgColor = Color.rgb(254, 243, 224) // Soft orange/amber
                badgeTextColor = Color.rgb(230, 81, 0)
            } else {
                statusText = "DUE"
                badgeBgColor = Color.rgb(253, 236, 236) // Soft red
                badgeTextColor = Color.rgb(217, 48, 37)
            }
        }
        
        val badgePaint = Paint().apply { isAntiAlias = true }
        val badgeRect = RectF(465f, cardTop + 10f, 535f, cardTop + 24f)
        badgePaint.color = badgeBgColor
        canvas1.drawRoundRect(badgeRect, 7f, 7f, badgePaint)
        
        badgePaint.color = badgeTextColor
        badgePaint.textSize = 7.5f
        badgePaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        badgePaint.textAlign = Paint.Align.CENTER
        canvas1.drawText(statusText, badgeRect.centerX(), badgeRect.centerY() + 2.5f, badgePaint)
        paint.textAlign = Paint.Align.LEFT // Restore paint default
        
        // 5. Line Items Table (Y = 245f)
        var currentY = 245f
        
        // Premium Table Header background
        paint.color = Color.rgb(240, 240, 245)
        canvas1.drawRect(50f, currentY, 545f, currentY + 22f, paint)
        
        paint.color = charcoalColor
        paint.textSize = 8.5f
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        canvas1.drawText("DESCRIPTION", 60f, currentY + 14f, paint)
        AndroidPdfInvoiceBitmapUtils.drawTextRightAligned(canvas1, "QTY", 360f, currentY + 14f, paint)
        AndroidPdfInvoiceBitmapUtils.drawTextRightAligned(canvas1, "UNIT PRICE", 450f, currentY + 14f, paint)
        AndroidPdfInvoiceBitmapUtils.drawTextRightAligned(canvas1, "TOTAL", 535f, currentY + 14f, paint)
        
        currentY += 22f
        
        // Table Rows
        data.items.forEachIndexed { idx, item ->
            // Background fill for alternating rows
            if (idx % 2 == 1) {
                paint.color = Color.rgb(252, 252, 254)
                canvas1.drawRect(50f, currentY, 545f, currentY + 24f, paint)
            }
            
            // Draw a very thin row outline or separator
            paint.color = borderGrayColor
            paint.strokeWidth = 0.5f
            canvas1.drawLine(50f, currentY + 24f, 545f, currentY + 24f, paint)
            
            paint.color = charcoalColor
            paint.textSize = 9f
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            
            // Truncate description if too long to avoid layout breaking
            val descText = if (item.description.length > 48) item.description.take(45) + "..." else item.description
            canvas1.drawText(descText, 60f, currentY + 15f, paint)
            
            // Draw Qty
            val qtyVal = item.quantity ?: 1.0
            val qtyText = if (qtyVal % 1.0 == 0.0) qtyVal.toInt().toString() else String.format("%.2f", qtyVal)
            paint.color = grayColor
            AndroidPdfInvoiceBitmapUtils.drawTextRightAligned(canvas1, qtyText, 360f, currentY + 15f, paint)
            
            // Draw Unit Price
            val unitPriceVal = item.unitPrice ?: item.amount
            val unitPriceText = "$${String.format("%.2f", unitPriceVal)}"
            AndroidPdfInvoiceBitmapUtils.drawTextRightAligned(canvas1, unitPriceText, 450f, currentY + 15f, paint)
            
            // Draw Line Total
            paint.color = charcoalColor
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            val amtText = "$${String.format("%.2f", item.amount)}"
            AndroidPdfInvoiceBitmapUtils.drawTextRightAligned(canvas1, amtText, 535f, currentY + 15f, paint)
            
            currentY += 24f
        }
        
        // Calculations Block (Y starts after items list)
        currentY += 15f
        
        val subtotal = data.items.sumOf { it.amount }
        val taxAmount = subtotal * (data.taxRate / 100.0)
        val totalAmount = subtotal + taxAmount - data.deposit
        
        // Calculations Container Rounded Box (Stripe/Wave style)
        val calcBoxWidth = 220f
        val calcBoxHeight = if (data.deposit > 0.0) 96f else 80f
        val calcBox = RectF(325f, currentY, 545f, currentY + calcBoxHeight)
        
        paint.color = lightGrayColor
        paint.style = Paint.Style.FILL
        canvas1.drawRoundRect(calcBox, 8f, 8f, paint)
        paint.color = borderGrayColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas1.drawRoundRect(calcBox, 8f, 8f, paint)
        paint.style = Paint.Style.FILL
        
        var calcY = currentY + 18f
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        
        canvas1.drawText("Subtotal", 340f, calcY, paint)
        AndroidPdfInvoiceBitmapUtils.drawTextRightAligned(canvas1, "$${String.format("%.2f", subtotal)}", 530f, calcY, paint)
        calcY += 16f
        
        canvas1.drawText("Tax (${data.taxRate}%)", 340f, calcY, paint)
        AndroidPdfInvoiceBitmapUtils.drawTextRightAligned(canvas1, "$${String.format("%.2f", taxAmount)}", 530f, calcY, paint)
        calcY += 16f
        
        if (data.deposit > 0.0) {
            canvas1.drawText("Deposit Paid", 340f, calcY, paint)
            paint.color = Color.rgb(30, 142, 62) // Green for deposit paid
            AndroidPdfInvoiceBitmapUtils.drawTextRightAligned(canvas1, "-$${String.format("%.2f", data.deposit)}", 530f, calcY, paint)
            paint.color = grayColor
            calcY += 16f
        }
        
        // Highlight Grand Total bottom bar with subtle top line
        paint.color = borderGrayColor
        paint.strokeWidth = 1f
        canvas1.drawLine(325f, calcY - 4f, 545f, calcY - 4f, paint)
        
        paint.color = charcoalColor
        paint.textSize = 10f
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        canvas1.drawText("TOTAL DUE", 340f, calcY + 12f, paint)
        
        paint.textSize = 12f
        paint.color = orangeColor
        val grandTotalText = "$${String.format("%.2f", totalAmount)}"
        AndroidPdfInvoiceBitmapUtils.drawTextRightAligned(canvas1, grandTotalText, 530f, calcY + 13f, paint)
        
        // Centered Footer Thank You Note
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        val thankYouText = "THANK YOU FOR YOUR BUSINESS!"
        val textWidth = paint.measureText(thankYouText)
        canvas1.drawText(thankYouText, (595f - textWidth) / 2f, 790f, paint)
        
        pdfDocument.finishPage(page1)
        AndroidPdfInvoiceGalleryPages.append(pdfDocument, context, data, paint, orangeColor, charcoalColor)

        // --- SAVE AND SHUT DOWN ---
        val internalDir = File(context.filesDir, "vault/invoices")
        if (!internalDir.exists()) internalDir.mkdirs()
        
        val file = File(internalDir, "${if (data.isEstimate) "Estimate" else "Invoice"}_${data.invoiceId}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file.absolutePath
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
}
