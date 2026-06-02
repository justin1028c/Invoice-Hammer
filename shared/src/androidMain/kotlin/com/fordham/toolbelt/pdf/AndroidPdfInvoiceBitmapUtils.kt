package com.fordham.toolbelt.pdf

import android.content.Context
import android.graphics.*
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File

internal object AndroidPdfInvoiceBitmapUtils {

    /**
     * Decodes a vault file path or content URI to a bitmap.
     * Uses [ImageDecoder] on API 28+ (HEIC/Samsung camera safe); legacy path uses BitmapFactory + EXIF.
     */
    fun decodeUriToBitmap(context: Context, uriStr: String?, targetW: Int = 240, targetH: Int = 130): Bitmap? {
        if (uriStr.isNullOrEmpty()) return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                decodeWithImageDecoder(context, uriStr, targetW, targetH)
            } else {
                decodeWithBitmapFactory(context, uriStr, targetW, targetH)
            }
        } catch (_: Exception) {
            decodeWithBitmapFactory(context, uriStr, targetW, targetH)
        }
    }

    fun decodeWithImageDecoder(context: Context, uriStr: String, targetW: Int, targetH: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        val source = if (uriStr.startsWith("/")) {
            ImageDecoder.createSource(File(uriStr))
        } else {
            ImageDecoder.createSource(context.contentResolver, Uri.parse(uriStr))
        }
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
            val width = info.size.width.coerceAtLeast(1)
            val height = info.size.height.coerceAtLeast(1)
            val sample = maxOf(
                1,
                minOf(width / targetW, height / targetH)
            )
            decoder.setTargetSampleSize(sample)
        }
        val rotationDeg = getExifRotation(context, uriStr)
        return rotateIfNeeded(bitmap, rotationDeg)
    }

    private fun getExifRotation(context: Context, uriStr: String): Int {
        return try {
            if (uriStr.startsWith("/")) {
                val exif = androidx.exifinterface.media.ExifInterface(uriStr)
                exifToDegrees(
                    exif.getAttributeInt(
                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                    )
                )
            } else {
                val uri = Uri.parse(uriStr)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = androidx.exifinterface.media.ExifInterface(stream)
                    exifToDegrees(
                        exif.getAttributeInt(
                            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                        )
                    )
                } ?: 0
            }
        } catch (_: Exception) {
            0
        }
    }

    fun decodeWithBitmapFactory(context: Context, uriStr: String, targetW: Int, targetH: Int): Bitmap? {
        return try {
            if (uriStr.startsWith("/")) {
                decodeFileWithExif(uriStr, targetW, targetH)
            } else {
                val uri = Uri.parse(uriStr)
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, boundsOpts)
                }
                val sampleSize = calculateInSampleSize(boundsOpts, targetW, targetH)
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                }
                val rotationDeg = context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = androidx.exifinterface.media.ExifInterface(stream)
                    exifToDegrees(
                        exif.getAttributeInt(
                            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                        )
                    )
                } ?: 0
                rotateIfNeeded(bitmap, rotationDeg)
            }
        } catch (_: Exception) {
            if (uriStr.startsWith("/")) decodeFileWithExif(uriStr, targetW, targetH) else null
        }
    }

    fun decodeFileWithExif(path: String, targetW: Int, targetH: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, boundsOpts)
        val sampleSize = calculateInSampleSize(boundsOpts, targetW, targetH)
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeFile(path, decodeOpts)
        val exif = androidx.exifinterface.media.ExifInterface(path)
        val rotationDeg = exifToDegrees(
            exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
        )
        return rotateIfNeeded(bitmap, rotationDeg)
    }

    fun rotateIfNeeded(bitmap: Bitmap?, rotationDeg: Int): Bitmap? {
        if (bitmap == null || rotationDeg == 0) return bitmap
        val matrix = Matrix()
        matrix.postRotate(rotationDeg.toFloat())
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    /** Converts an ExifInterface orientation constant to a clockwise rotation in degrees. */
    fun exifToDegrees(exifOrientation: Int): Int = when (exifOrientation) {
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
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
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
     * Fits the full logo inside [slotW]×[slotH] with aspect ratio preserved (letterboxed on white).
     * Unlike [getResizedBitmap], this never crops wide/tall logos.
     */
    fun prepareLogoContained(source: Bitmap, slotW: Int, slotH: Int): Bitmap {
        val output = Bitmap.createBitmap(slotW, slotH, Bitmap.Config.ARGB_8888)
        val logoCanvas = Canvas(output)
        logoCanvas.drawColor(Color.WHITE)
        val scale = minOf(slotW.toFloat() / source.width, slotH.toFloat() / source.height)
        val drawW = (source.width * scale).coerceAtLeast(1f)
        val drawH = (source.height * scale).coerceAtLeast(1f)
        val left = (slotW - drawW) / 2f
        val top = (slotH - drawH) / 2f
        val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        logoCanvas.drawBitmap(source, null, RectF(left, top, left + drawW, top + drawH), logoPaint)
        return output
    }

    /**
     * Scales a bitmap to exactly newWidth x newHeight using bilinear filtering (filter=true).
     * Uses fit-inside (Math.min) to avoid stretching, then centre-crops to the exact cell.
     */
    fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
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
    fun prepareForCell(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
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
    
    fun drawTextRightAligned(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(text, x, y, paint)
        paint.textAlign = Paint.Align.LEFT
    }
}

