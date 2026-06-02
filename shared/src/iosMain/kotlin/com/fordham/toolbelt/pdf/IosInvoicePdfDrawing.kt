package com.fordham.toolbelt.pdf

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
internal object IosInvoicePdfDrawing {

    /** Aspect-fit logo into a fixed slot (matches Android [prepareLogoContained]). */
    fun drawLogoContained(
        image: UIImage,
        slotX: Double,
        slotY: Double,
        slotW: Double,
        slotH: Double
    ) {
        val (imgW, imgH) = image.size.useContents { width to height }
        if (imgW <= 0.0 || imgH <= 0.0) return
        val scale = minOf(slotW / imgW, slotH / imgH)
        val drawW = imgW * scale
        val drawH = imgH * scale
        val drawX = slotX + (slotW - drawW) / 2.0
        val drawY = slotY + (slotH - drawH) / 2.0
        image.drawInRect(CGRectMake(drawX, drawY, drawW, drawH))
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
    fun drawAspectFillImage(image: UIImage, rect: CValue<CGRect>) {
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

}
