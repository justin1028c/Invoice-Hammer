package com.fordham.toolbelt.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.fordham.toolbelt.domain.model.BentoReportData
import com.fordham.toolbelt.util.SecurityManager
import java.io.File
import java.io.FileOutputStream

class BentoReportEngine(
    private val context: Context,
    private val securityManager: SecurityManager
) {
    fun generateBentoPdf(data: BentoReportData, outputFile: File? = null): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)
        
        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Header Background (Slate-800)
        paint.color = Color.parseColor("#1E293B")
        paint.style = Paint.Style.FILL
        val headerRect = RectF(45f, 40f, 550f, 110f)
        canvas.drawRoundRect(headerRect, 8f, 8f, paint)

        // Header Text - Left
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("INVOICE HAMMER", 60f, 68f, paint)
        paint.textSize = 18f
        canvas.drawText("Bento Business Report", 60f, 94f, paint)

        // Header Text - Right
        paint.isFakeBoldText = false
        paint.textSize = 8f
        paint.color = Color.parseColor("#94A3B8")
        canvas.drawText("FINANCIAL YEAR YTD", 400f, 68f, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f
        val dateStr = com.fordham.toolbelt.util.DateTimeUtil.getNowFormatted()
        canvas.drawText("Generated: $dateStr", 400f, 90f, paint)

        // Calculations
        val marginPercent = if (data.grossIncome > 0.0) {
            ((data.netProfit / data.grossIncome) * 100).toInt()
        } else {
            0
        }

        // Card 1: Net Profit (Large)
        drawCard(
            canvas, paint,
            left = 45f, top = 130f, right = 285f, bottom = 250f,
            bgColor = "#DCFCE7", borderColor = "#86EFAC",
            title = "NET PROFIT YTD", titleColor = "#166534",
            value = formatCurrency(data.netProfit), valueColor = "#15803D", valueSize = 20f,
            subtext = "Status: Profitable YTD", subtextColor = "#15803D"
        )

        // Card 2: Gross Income
        drawCard(
            canvas, paint,
            left = 310f, top = 130f, right = 550f, bottom = 185f,
            bgColor = "#DBEAFE", borderColor = "#BFDBFE",
            title = "GROSS INCOME", titleColor = "#1E3A8A",
            value = formatCurrency(data.grossIncome), valueColor = "#1D4ED8", valueSize = 14f,
            subtext = null, subtextColor = null
        )

        // Card 3: Margin
        drawCard(
            canvas, paint,
            left = 310f, top = 195f, right = 550f, bottom = 250f,
            bgColor = "#F3E8FF", borderColor = "#E9D5FF",
            title = "PROFIT MARGIN", titleColor = "#581C87",
            value = "$marginPercent%", valueColor = "#7E22CE", valueSize = 14f,
            subtext = null, subtextColor = null
        )

        // Card 4: Total Expenses
        drawCard(
            canvas, paint,
            left = 45f, top = 265f, right = 285f, bottom = 335f,
            bgColor = "#FEE2E2", borderColor = "#FCA5A5",
            title = "TOTAL EXPENSES", titleColor = "#991B1B",
            value = formatCurrency(data.expenses), valueColor = "#B91C1C", valueSize = 14f,
            subtext = null, subtextColor = null
        )

        // Card 5: Operations Summary
        drawCard(
            canvas, paint,
            left = 310f, top = 265f, right = 550f, bottom = 335f,
            bgColor = "#F8FAFC", borderColor = "#E2E8F0",
            title = "OPERATIONS SUMMARY", titleColor = "#475569",
            value = "${data.invoices.size} Invoices | ${data.receiptCount} Receipts", valueColor = "#1E293B", valueSize = 12f,
            subtext = "Clean audit trail active", subtextColor = "#64748B"
        )

        // Recent Invoices Table
        paint.color = Color.parseColor("#334155")
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("RECENT PAID INVOICES", 45f, 370f, paint)

        // Table Header
        paint.color = Color.parseColor("#F1F5F9")
        paint.style = Paint.Style.FILL
        canvas.drawRect(45f, 385f, 550f, 405f, paint)

        paint.color = Color.parseColor("#475569")
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("Date", 55f, 398f, paint)
        canvas.drawText("Client", 140f, 398f, paint)
        canvas.drawText("Description", 260f, 398f, paint)
        canvas.drawText("Amount", 480f, 398f, paint)

        // Table Rows
        var y = 422f
        paint.isFakeBoldText = false
        paint.textSize = 9f

        val sortedInvoices = data.invoices.sortedByDescending { it.date }
        val maxRows = 15
        val displayInvoices = sortedInvoices.take(maxRows)

        for (invoice in displayInvoices) {
            paint.color = Color.parseColor("#334155")
            canvas.drawText(invoice.date.take(10), 55f, y, paint)
            
            val clientDisplay = if (invoice.clientName.length > 18) invoice.clientName.take(16) + ".." else invoice.clientName
            canvas.drawText(clientDisplay, 140f, y, paint)
            
            val summaryDisplay = if (invoice.itemsSummary.length > 40) invoice.itemsSummary.take(37) + "..." else invoice.itemsSummary
            canvas.drawText(summaryDisplay, 260f, y, paint)
            
            canvas.drawText(formatCurrency(invoice.totalAmount), 480f, y, paint)

            // Draw line separator
            paint.color = Color.parseColor("#F1F5F9")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawLine(45f, y + 6f, 550f, y + 6f, paint)
            paint.style = Paint.Style.FILL

            y += 24f
        }

        if (sortedInvoices.size > maxRows) {
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 8f
            canvas.drawText("+ ${sortedInvoices.size - maxRows} more paid invoices recorded YTD", 45f, y + 10f, paint)
        }

        pdfDocument.finishPage(page)

        val file = outputFile ?: File(context.cacheDir, "Bento_Report.pdf")
        file.parentFile?.mkdirs()
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }

    private fun drawCard(
        canvas: Canvas,
        paint: Paint,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        bgColor: String,
        borderColor: String,
        title: String,
        titleColor: String,
        value: String,
        valueColor: String,
        valueSize: Float,
        subtext: String?,
        subtextColor: String?
    ) {
        paint.color = Color.parseColor(bgColor)
        paint.style = Paint.Style.FILL
        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.color = Color.parseColor(borderColor)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor(titleColor)
        paint.textSize = 8f
        paint.isFakeBoldText = true
        canvas.drawText(title, left + 15f, top + 18f, paint)

        paint.color = Color.parseColor(valueColor)
        paint.textSize = valueSize
        paint.isFakeBoldText = true
        
        val isDoubleRow = (bottom - top) > 60f
        val valY = if (isDoubleRow) top + 52f else top + 38f
        canvas.drawText(value, left + 15f, valY, paint)

        if (subtext != null && subtextColor != null && isDoubleRow) {
            paint.color = Color.parseColor(subtextColor)
            paint.textSize = 8f
            paint.isFakeBoldText = false
            canvas.drawText(subtext, left + 15f, bottom - 14f, paint)
        }
    }

    private fun formatCurrency(amount: Double): String {
        return java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US).format(amount)
    }
}

