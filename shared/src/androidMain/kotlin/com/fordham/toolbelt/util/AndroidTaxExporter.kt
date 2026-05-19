package com.fordham.toolbelt.util

import android.content.Context
import com.fordham.toolbelt.domain.model.DocumentCategory
import com.fordham.toolbelt.domain.model.DocumentExportOutcome
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.model.TaxExportOutcome
import com.fordham.toolbelt.domain.repository.DocumentExporter
import com.fordham.toolbelt.pdf.BentoReportEngine
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AndroidTaxExporter(
    private val context: Context,
    private val bentoReportEngine: BentoReportEngine,
    private val documentExporter: DocumentExporter
) : TaxExporter {

    override suspend fun exportBentoReport(
        invoices: List<Invoice>,
        receipts: List<ReceiptItem>
    ): TaxExportOutcome = try {
        val paidInvoices = invoices.filter { it.isPaid && !it.isEstimate }
        val totalIncome = paidInvoices.sumOf { it.totalAmount }
        val totalExpenses = receipts.sumOf { it.totalPrice }

        val reportData = com.fordham.toolbelt.domain.model.BentoReportData(
            netProfit = totalIncome - totalExpenses,
            grossIncome = totalIncome,
            expenses = totalExpenses,
            invoices = paidInvoices,
            receiptCount = receipts.size
        )

        val pdfFile = bentoReportEngine.generateBentoPdf(reportData)
            ?: throw Exception("Failed to generate Bento PDF")

        // Mirror to /storage/emulated/0/Documents/InvoiceHammer/Reports/ for user
        // visibility. Failure is non-fatal — caller still gets the shareable cache path.
        when (val outcome = documentExporter.publish(
            sourcePath = pdfFile.absolutePath,
            category = DocumentCategory.Reports,
            displayName = pdfFile.name
        )) {
            is DocumentExportOutcome.Failure -> println("ANDROID_TAX_EXPORTER: Reports publish failed: ${outcome.error.value}")
            is DocumentExportOutcome.Success -> Unit
        }

        TaxExportOutcome.Success(pdfFile.absolutePath)
    } catch (e: Exception) {
        TaxExportOutcome.Failure(
            com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to generate report summary")
        )
    }

    override suspend fun exportFullTaxBundle(
        invoices: List<Invoice>,
        receipts: List<ReceiptItem>
    ): TaxExportOutcome = try {
        val name = "Tax_Bundle_${System.currentTimeMillis()}.zip"
        val cacheFile = File(context.cacheDir, name)

        ZipOutputStream(FileOutputStream(cacheFile).buffered()).use { zos ->
            val addedPaths = mutableSetOf<String>()

            // 1. Add Bento PDF Summary
            val summaryResult = exportBentoReport(invoices, receipts)
            if (summaryResult is TaxExportOutcome.Success) {
                val pdfEntry = "Business_Report.pdf"
                zos.putNextEntry(ZipEntry(pdfEntry))
                File(summaryResult.path).inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
                addedPaths.add(pdfEntry)
            }

            // 2. Add Invoice PDFs
            invoices.filter { it.pdfPath.isNotEmpty() }.distinctBy { it.pdfPath }.forEach { invoice ->
                val f = File(invoice.pdfPath)
                if (f.exists() && f.isFile && f.length() > 0) {
                    val entryPath = "Invoices/${f.name}"
                    if (addedPaths.add(entryPath)) {
                        try {
                            zos.putNextEntry(ZipEntry(entryPath))
                            f.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        } catch (e: Exception) {
                            try { zos.closeEntry() } catch (_: Exception) {}
                        }
                    }
                }
            }
        }

        // 3. Publish ZIP to Documents/InvoiceHammer/TaxBundles/
        when (val outcome = documentExporter.publish(
            sourcePath = cacheFile.absolutePath,
            category = DocumentCategory.TaxBundles,
            displayName = name
        )) {
            is DocumentExportOutcome.Failure -> println("ANDROID_TAX_EXPORTER: TaxBundle publish failed: ${outcome.error.value}")
            is DocumentExportOutcome.Success -> Unit
        }

        TaxExportOutcome.Success(cacheFile.absolutePath)
    } catch (e: Exception) {
        TaxExportOutcome.Failure(
            com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to assemble tax bundle zip")
        )
    }
}
