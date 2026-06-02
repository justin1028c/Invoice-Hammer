from pathlib import Path

p = Path("shared/src/androidMain/kotlin/com/fordham/toolbelt/pdf/AndroidPdfInvoiceEngine.kt")
text = p.read_text(encoding="utf-8")
marker = "        // --- PAGE 2+: JOB SITE GALLERY"
idx = text.find(marker)
if idx < 0:
    raise SystemExit("gallery marker not found")
head = text[:idx].rstrip() + "\n        AndroidPdfInvoiceGalleryPages.append(pdfDocument, context, data, paint, orangeColor, charcoalColor)\n\n"
tail = text[idx:]
# remove gallery block until SAVE AND SHUT DOWN
save_marker = "        // --- SAVE AND SHUT DOWN ---"
sidx = tail.find(save_marker)
gallery_body = tail[:sidx]
head_part = text[:idx]
# extract gallery implementation from gallery_body for new file
gallery_impl = gallery_body.replace("        // --- PAGE 2+: JOB SITE GALLERY (standard page size; decode at 2× then fit cells) ---\n", "")
gallery_file = '''package com.fordham.toolbelt.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.fordham.toolbelt.domain.model.InvoiceData
import com.fordham.toolbelt.domain.model.JobPhotoPhase
import com.fordham.toolbelt.domain.model.buildJobPhotoGalleryRows

internal object AndroidPdfInvoiceGalleryPages {

    fun append(
        pdfDocument: PdfDocument,
        context: Context,
        data: InvoiceData,
        paint: Paint,
        orangeColor: Int,
        charcoalColor: Int
    ) {
''' + gallery_impl.replace("        val galleryRows", "        val galleryRows").replace("        if (galleryRows", "        if (galleryRows") + "    }\n}\n"
# fix: gallery_impl still has wrong indentation level - it's already 8 spaces for body inside generatePdf
# The gallery_impl starts with val galleryRows at 8 spaces - inside object method needs 8 spaces - ok

tail_rest = tail[sidx:]
head = head_part.rstrip() + "\n        AndroidPdfInvoiceGalleryPages.append(pdfDocument, context, data, paint, orangeColor, charcoalColor)\n\n" + tail_rest
p.write_text(head, encoding="utf-8")
Path("shared/src/androidMain/kotlin/com/fordham/toolbelt/pdf/AndroidPdfInvoiceGalleryPages.kt").write_text(gallery_file, encoding="utf-8")
print("engine", len(head.splitlines()), "gallery", len(gallery_file.splitlines()))
