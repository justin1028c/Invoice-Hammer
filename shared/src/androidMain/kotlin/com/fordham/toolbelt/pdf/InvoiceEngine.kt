package com.fordham.toolbelt.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.fordham.toolbelt.domain.model.InvoiceData
import com.fordham.toolbelt.util.SecurityManager
import java.io.File
import java.io.FileOutputStream

class InvoiceEngine(
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
        
        // Color Tokens
        val orangeColor = Color.rgb(252, 102, 0)
        val charcoalColor = Color.rgb(30, 30, 30)
        val grayColor = Color.rgb(100, 100, 100)
        val lightGrayColor = Color.rgb(245, 245, 245)
        
        // 1. Draw Business Details (Top Left)
        paint.color = orangeColor
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val bizName = data.settings.businessName.ifBlank { "INVOICE HAMMER" }.uppercase()
        canvas1.drawText(bizName, 50f, 60f, paint)
        
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val slogan = data.settings.businessSlogan.ifBlank { "Professional Field Services" }
        canvas1.drawText(slogan, 50f, 75f, paint)
        
        paint.color = charcoalColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var contactOffset = 90f
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
        val logoBitmap = decodeUriToBitmap(context, data.logoUriString ?: data.settings.logoUri, targetW = 160, targetH = 160)
        if (logoBitmap != null) {
            val scaledLogo = getResizedBitmap(logoBitmap, 80, 80)
            canvas1.drawBitmap(scaledLogo, 465f, 40f, paint)
            // Draw a thin charcoal border around logo
            paint.color = charcoalColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas1.drawRect(465f, 40f, 545f, 120f, paint)
            paint.style = Paint.Style.FILL
        }
        
        // Divider line
        paint.color = orangeColor
        paint.strokeWidth = 3f
        canvas1.drawLine(50f, 145f, 545f, 145f, paint)
        
        // 3. Client & Metadata Split Boxes (Y = 160f)
        // Billed To (Left Column)
        paint.color = orangeColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("BILLED TO:", 50f, 175f, paint)
        
        paint.color = charcoalColor
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText(data.clientName.uppercase(), 50f, 190f, paint)
        
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText(data.clientAddress.ifBlank { "No Job Address Provided" }, 50f, 203f, paint)
        
        // Document Meta (Right Column)
        paint.color = orangeColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("DOCUMENT DETAILS:", 330f, 175f, paint)
        
        paint.color = charcoalColor
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val docType = if (data.isEstimate) "ESTIMATE" else "INVOICE"
        canvas1.drawText("$docType #${data.invoiceId.take(8).uppercase()}", 330f, 190f, paint)
        
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("DATE: ${data.date}", 330f, 203f, paint)
        
        // 4. Line Items Table (Y = 230f)
        var currentY = 230f
        
        // Table Header
        paint.color = charcoalColor
        canvas1.drawRect(50f, currentY, 545f, currentY + 20f, paint)
        
        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("DESCRIPTION", 60f, currentY + 13f, paint)
        canvas1.drawText("CATEGORY", 320f, currentY + 13f, paint)
        drawTextRightAligned(canvas1, "TOTAL", 535f, currentY + 13f, paint)
        
        currentY += 20f
        
        // Table Rows
        data.items.forEachIndexed { idx, item ->
            // Background fill for alternating rows
            if (idx % 2 == 1) {
                paint.color = lightGrayColor
                canvas1.drawRect(50f, currentY, 545f, currentY + 22f, paint)
            }
            
            paint.color = charcoalColor
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            
            // Truncate description if too long
            val descText = if (item.description.length > 40) item.description.take(37) + "..." else item.description
            canvas1.drawText(descText, 60f, currentY + 14f, paint)
            
            paint.color = grayColor
            canvas1.drawText(item.category.uppercase(), 320f, currentY + 14f, paint)
            
            paint.color = charcoalColor
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val amtText = "$${String.format("%.2f", item.amount)}"
            drawTextRightAligned(canvas1, amtText, 535f, currentY + 14f, paint)
            
            // Draw a thin horizontal dividing line
            paint.color = Color.rgb(220, 220, 220)
            paint.strokeWidth = 0.5f
            canvas1.drawLine(50f, currentY + 22f, 545f, currentY + 22f, paint)
            
            currentY += 22f
        }
        
        // Calculations Block (Y starts after items list)
        currentY += 15f
        
        val subtotal = data.items.sumOf { it.amount }
        val taxAmount = subtotal * (data.taxRate / 100.0)
        val totalAmount = subtotal + taxAmount - data.deposit
        
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        canvas1.drawText("Subtotal:", 330f, currentY, paint)
        drawTextRightAligned(canvas1, "$${String.format("%.2f", subtotal)}", 535f, currentY, paint)
        currentY += 14f
        
        canvas1.drawText("Tax (${data.taxRate}%):", 330f, currentY, paint)
        drawTextRightAligned(canvas1, "$${String.format("%.2f", taxAmount)}", 535f, currentY, paint)
        currentY += 14f
        
        if (data.deposit > 0.0) {
            canvas1.drawText("Deposit Collected:", 330f, currentY, paint)
            drawTextRightAligned(canvas1, "-$${String.format("%.2f", data.deposit)}", 535f, currentY, paint)
            currentY += 14f
        }
        
        // Grand Total Box
        currentY += 5f
        paint.color = orangeColor
        canvas1.drawRect(330f, currentY, 545f, currentY + 26f, paint)
        
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("TOTAL DUE:", 340f, currentY + 16f, paint)
        
        paint.textSize = 12f
        val grandTotalText = "$${String.format("%.2f", totalAmount)}"
        drawTextRightAligned(canvas1, grandTotalText, 535f, currentY + 17f, paint)
        
        // Centered Footer Thank You Note
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("THANK YOU FOR YOUR BUSINESS!", 195f, 790f, paint)
        
        pdfDocument.finishPage(page1)
        
        // --- PAGE 2+: HIGH-RESOLUTION JOB SITE GALLERY ---
        // Galaxy-class screens (S25 Ultra: 500 DPI) upscale a standard 595×842 pt page ~3×.
        // Standard 240×130 pt cells = only 240×130 actual pixels → looks blocky when zoomed.
        // Fix: use a 3× resolution gallery page (1785×2526 pt) so each photo cell is
        // 720×390 real pixels — 9× more data, crisp on any screen.
        if (data.photoUris.isNotEmpty()) {
            val SCALE = 3f
            val galleryPageInfo = PdfDocument.PageInfo.Builder(
                (595 * SCALE).toInt(),   // 1785
                (842 * SCALE).toInt(),   // 2526
                1
            ).create()

            // Scaled color tokens (reuse from page 1)
            var photoIdx = 0
            var pageNum = 2

            while (photoIdx < data.photoUris.size) {
                val pageG = pdfDocument.startPage(galleryPageInfo)
                val canvasG = pageG.canvas

                // Reset Paint for Gallery
                paint.reset()
                paint.isAntiAlias = true
                paint.isFilterBitmap = true
                paint.isDither = true

                // Page Header  (all coords × SCALE)
                paint.color = orangeColor
                paint.textSize = 18f * SCALE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvasG.drawText("JOB SITE GALLERY - PAGE ${pageNum - 1}", 50f * SCALE, 60f * SCALE, paint)

                paint.color = charcoalColor
                paint.strokeWidth = 2f * SCALE
                canvasG.drawLine(50f * SCALE, 72f * SCALE, 545f * SCALE, 72f * SCALE, paint)

                // Draw 2×4 grid (up to 8 photos per page)
                // Cell dimensions: 720×390 real pixels (was 240×130 at 1×)
                val cellW = 240f * SCALE   // 720
                val cellH = 130f * SCALE   // 390
                val colGap = 255f * SCALE  // 765  (col spacing matches old 255)
                val rowGap = 165f * SCALE  // 495  (row spacing matches old 165)
                val marginX = 50f * SCALE  // 150
                val gridStartY = 90f * SCALE // 270

                val photosOnThisPage = minOf(8, data.photoUris.size - photoIdx)
                for (i in 0 until photosOnThisPage) {
                    val currentUri = data.photoUris[photoIdx + i]
                    val row = i / 2
                    val col = i % 2

                    val x = marginX + col * colGap
                    val y = gridStartY + row * rowGap

                    // Decode at full cell resolution (720×390) for crisp output
                    val photoBitmap = decodeUriToBitmap(context, currentUri, targetW = 720, targetH = 390)
                    if (photoBitmap != null) {
                        // Aspect-fill crop to the cell aspect ratio THEN downscale once to the
                        // exact cell pixel size before blitting. This avoids both the anisotropic
                        // stretching (cell is 1.85:1, camera photos are usually 4:3) and the
                        // over-2x bilinear minification that caused the smeared appearance.
                        val ready = prepareForCell(photoBitmap, cellW.toInt(), cellH.toInt())
                        canvasG.drawBitmap(ready, x, y, paint)

                        // Border around photo
                        paint.color = charcoalColor
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 1.5f * SCALE
                        canvasG.drawRect(x, y, x + cellW, y + cellH, paint)
                        paint.style = Paint.Style.FILL

                        // Caption: left col = BEFORE (charcoal), right col = AFTER (orange)
                        if (col == 0) {
                            paint.color = charcoalColor
                            paint.textSize = 8f * SCALE
                            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            canvasG.drawText("[ BEFORE WORK ]", x + 5f * SCALE, y + cellH + 13f * SCALE, paint)
                        } else {
                            paint.color = orangeColor
                            paint.textSize = 8f * SCALE
                            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            canvasG.drawText("[ AFTER WORK ]", x + 5f * SCALE, y + cellH + 13f * SCALE, paint)
                        }
                    }
                }

                pdfDocument.finishPage(pageG)
                photoIdx += photosOnThisPage
                pageNum++
            }
        }

        
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
    
    /**
     * Decodes a URI or file path to a Bitmap, pre-sampled to avoid loading massive camera
     * images at full resolution. inSampleSize is calculated so the decoded bitmap is
     * at least 2x the target size, giving Bitmap.createScaledBitmap enough data for
     * high-quality bilinear filtering without wasting memory.
     *
     * Also reads EXIF orientation and rotates the bitmap to correct sideways/upside-down
     * photos — BitmapFactory.decodeStream ignores EXIF tags by default.
     */
    private fun decodeUriToBitmap(context: Context, uriStr: String?, targetW: Int = 240, targetH: Int = 130): Bitmap? {
        if (uriStr.isNullOrEmpty()) return null
        return try {
            val uri = android.net.Uri.parse(uriStr)
            // First pass: read bounds only to calculate inSampleSize
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
            val sampleSize = calculateInSampleSize(boundsOpts, targetW, targetH)
            // Second pass: decode at reduced size
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
            // Third pass: read EXIF orientation and correct rotation
            val rotationDeg = context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = androidx.exifinterface.media.ExifInterface(stream)
                exifToDegrees(exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                ))
            } ?: 0
            if (bitmap != null && rotationDeg != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotationDeg.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            // Fallback: file path
            try {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(uriStr, boundsOpts)
                val sampleSize = calculateInSampleSize(boundsOpts, targetW, targetH)
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = BitmapFactory.decodeFile(uriStr, decodeOpts)
                val exif = androidx.exifinterface.media.ExifInterface(uriStr)
                val rotationDeg = exifToDegrees(exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                ))
                if (bitmap != null && rotationDeg != 0) {
                    val matrix = Matrix()
                    matrix.postRotate(rotationDeg.toFloat())
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                } else {
                    bitmap
                }
            } catch (ex: Exception) {
                null
            }
        }
    }

    /** Converts an ExifInterface orientation constant to a clockwise rotation in degrees. */
    private fun exifToDegrees(exifOrientation: Int): Int = when (exifOrientation) {
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90  -> 90
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }


    /**
     * Calculates the largest inSampleSize that is a power of 2 and keeps the decoded
     * bitmap at least 2x the target dimensions, so bilinear filtering has enough
     * source data to produce a crisp result.
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val rawH = options.outHeight
        val rawW = options.outWidth
        var inSampleSize = 1
        if (rawH > reqHeight * 2 || rawW > reqWidth * 2) {
            val halfH = rawH / 2
            val halfW = rawW / 2
            while (halfH / inSampleSize >= reqHeight * 2 && halfW / inSampleSize >= reqWidth * 2) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Scales a bitmap to exactly newWidth x newHeight using bilinear filtering (filter=true).
     * Uses fit-inside (Math.min) to avoid stretching, then centre-crops to the exact cell.
     */
    private fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val scaleWidth = newWidth.toFloat() / bm.width
        val scaleHeight = newHeight.toFloat() / bm.height
        // fit-inside: pick the SMALLER scale so the whole image fits, then crop to cell
        val scale = minOf(scaleWidth, scaleHeight)
        val fittedW = (bm.width * scale).toInt().coerceAtLeast(1)
        val fittedH = (bm.height * scale).toInt().coerceAtLeast(1)
        // bilinear filtering via filter=true for crispness
        val fitted = Bitmap.createScaledBitmap(bm, fittedW, fittedH, true)
        // Centre-crop to exact cell dimensions
        val cropX = ((fittedW - newWidth) / 2).coerceAtLeast(0)
        val cropY = ((fittedH - newHeight) / 2).coerceAtLeast(0)
        val safeW = minOf(newWidth, fittedW - cropX)
        val safeH = minOf(newHeight, fittedH - cropY)
        return Bitmap.createBitmap(fitted, cropX, cropY, safeW, safeH)
    }

    /**
     * Aspect-fill (centre-crop) the source bitmap to a target pixel size, scaling down
     * in 2× steps when the source is much larger than the target.
     *
     * Why this fixes the smeared photos:
     *   - `Canvas.drawBitmap` with src/dst Rects stretches anisotropically when the
     *     source aspect ratio doesn't match the cell. Centre-cropping kills the smear
     *     from that stretch.
     *   - A single bilinear pass that compresses more than 2× per axis looks soft
     *     because bilinear only samples 4 texels. Progressive 2× downsampling samples
     *     the whole pixel neighbourhood, matching standard mipmap quality.
     */
    private fun prepareForCell(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        if (targetW <= 0 || targetH <= 0) return source

        val srcW = source.width
        val srcH = source.height
        if (srcW <= 0 || srcH <= 0) return source

        // 1) Aspect-fill: pick the LARGER scale so the image covers the cell, then crop.
        val srcAspect = srcW.toFloat() / srcH.toFloat()
        val dstAspect = targetW.toFloat() / targetH.toFloat()
        val cropW: Int
        val cropH: Int
        if (srcAspect > dstAspect) {
            // Source is wider than cell — trim the sides.
            cropH = srcH
            cropW = (srcH.toFloat() * dstAspect).toInt().coerceAtLeast(1)
        } else {
            // Source is taller than cell — trim the top/bottom.
            cropW = srcW
            cropH = (srcW.toFloat() / dstAspect).toInt().coerceAtLeast(1)
        }
        val cropX = ((srcW - cropW) / 2).coerceAtLeast(0)
        val cropY = ((srcH - cropH) / 2).coerceAtLeast(0)
        var working = Bitmap.createBitmap(source, cropX, cropY, cropW, cropH)

        // 2) Progressive 2× downsample until we're within one bilinear hop of the cell.
        while (working.width >= targetW * 2 && working.height >= targetH * 2) {
            val halved = Bitmap.createScaledBitmap(
                working,
                working.width / 2,
                working.height / 2,
                true
            )
            if (halved !== working) {
                working.recycle()
                working = halved
            } else break
        }

        // 3) Final exact-size scale (≤2× per axis at this point) using bilinear.
        if (working.width != targetW || working.height != targetH) {
            val finalScaled = Bitmap.createScaledBitmap(working, targetW, targetH, true)
            if (finalScaled !== working) {
                working.recycle()
                working = finalScaled
            }
        }
        return working
    }
    
    private fun drawTextRightAligned(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(text, x, y, paint)
        paint.textAlign = Paint.Align.LEFT
    }
}
