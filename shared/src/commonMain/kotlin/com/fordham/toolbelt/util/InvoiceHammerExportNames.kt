package com.fordham.toolbelt.util

object InvoiceHammerExportNames {
    fun bentoReportPdf(): String = "Bento_Report_${DateTimeUtil.exportFileStamp()}.pdf"

    fun taxBundleZip(): String = "Tax_Bundle_${DateTimeUtil.exportFileStamp()}.zip"
}
