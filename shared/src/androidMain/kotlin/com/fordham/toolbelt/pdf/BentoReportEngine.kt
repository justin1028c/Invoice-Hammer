package com.fordham.toolbelt.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.fordham.toolbelt.domain.model.BentoReportData
import com.fordham.toolbelt.util.SecurityManager
import java.io.File
import java.io.FileOutputStream

class BentoReportEngine(
    private val context: Context,
    private val securityManager: SecurityManager
) {
    fun generateBentoPdf(data: BentoReportData): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        paint.color = Color.BLACK
        paint.textSize = 24f
        canvas.drawText("Bento Business Report", 50f, 50f, paint)
        
        paint.textSize = 14f
        canvas.drawText("Gross Income: ${data.grossIncome}", 50f, 100f, paint)
        canvas.drawText("Expenses: ${data.expenses}", 50f, 120f, paint)
        canvas.drawText("Net Profit: ${data.netProfit}", 50f, 140f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Bento_Report.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
}
