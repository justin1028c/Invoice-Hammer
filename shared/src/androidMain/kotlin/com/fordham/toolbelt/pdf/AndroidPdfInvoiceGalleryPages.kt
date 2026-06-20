package com.fordham.toolbelt.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.fordham.toolbelt.domain.model.InvoiceData
import com.fordham.toolbelt.domain.model.JobPhotoPhase
import com.fordham.toolbelt.domain.model.buildJobPhotoGalleryRows
import com.fordham.toolbelt.util.UserFacingCopy

internal object AndroidPdfInvoiceGalleryPages {

    fun append(
        pdfDocument: PdfDocument,
        context: Context,
        data: InvoiceData,
        paint: Paint,
        orangeColor: Int,
        charcoalColor: Int
    ) {
        val galleryRows = buildJobPhotoGalleryRows(data.jobSitePhotos)
        if (galleryRows.isNotEmpty()) {
            val pdf = UserFacingCopy.Pdf
            val galleryPageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val cellW = 240f
            val cellH = 130f
            val colGap = 255f
            val rowGap = 165f
            val marginX = 50f
            val gridStartY = 90f
            val decodeW = (cellW * 2).toInt()
            val decodeH = (cellH * 2).toInt()

            var rowIdx = 0
            var pageNum = 2

            while (rowIdx < galleryRows.size) {
                val pageG = pdfDocument.startPage(galleryPageInfo)
                val canvasG = pageG.canvas

                paint.reset()
                paint.isAntiAlias = true
                paint.isFilterBitmap = true
                paint.isDither = true

                paint.color = orangeColor
                paint.textSize = 18f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvasG.drawText(pdf.galleryPageTitle(pageNum - 1), marginX, 60f, paint)

                paint.color = charcoalColor
                paint.strokeWidth = 2f
                canvasG.drawLine(marginX, 72f, 545f, 72f, paint)

                val rowsOnThisPage = minOf(4, galleryRows.size - rowIdx)
                for (r in 0 until rowsOnThisPage) {
                    val galleryRow = galleryRows[rowIdx + r]
                    val y = gridStartY + r * rowGap

                    fun drawGalleryCell(uri: String, col: Int, phase: JobPhotoPhase) {
                        val x = marginX + col * colGap
                        val photoBitmap = AndroidPdfInvoiceBitmapUtils.decodeUriToBitmap(context, uri, targetW = decodeW, targetH = decodeH)
                            ?: return
                        val ready = AndroidPdfInvoiceBitmapUtils.prepareForCell(photoBitmap, decodeW, decodeH)
                        if (ready !== photoBitmap) {
                            photoBitmap.recycle()
                        }
                        val dst = RectF(x, y, x + cellW, y + cellH)
                        canvasG.drawBitmap(ready, null, dst, paint)
                        ready.recycle()
                        paint.color = charcoalColor
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 1.5f
                        canvasG.drawRect(dst, paint)
                        paint.style = Paint.Style.FILL
                        paint.textSize = 8f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        if (phase == JobPhotoPhase.Before) {
                            paint.color = charcoalColor
                            canvasG.drawText(pdf.beforeWorkCaption(), x + 5f, y + cellH + 13f, paint)
                        } else {
                            paint.color = orangeColor
                            canvasG.drawText(pdf.afterWorkCaption(), x + 5f, y + cellH + 13f, paint)
                        }
                    }

                    galleryRow.beforeUri?.let { drawGalleryCell(it, col = 0, phase = JobPhotoPhase.Before) }
                    galleryRow.afterUri?.let { drawGalleryCell(it, col = 1, phase = JobPhotoPhase.After) }
                }

                pdfDocument.finishPage(pageG)
                rowIdx += rowsOnThisPage
                pageNum++
            }
        }

        
    }
}
