package com.fordham.toolbelt.util

import android.content.Context
import com.fordham.toolbelt.domain.model.DocumentCategory
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.model.TaxExportOutcome
import com.fordham.toolbelt.domain.repository.BentoReportGenerator
import com.fordham.toolbelt.domain.repository.DocumentExporter
import java.io.File

class AndroidTaxExporter(
    private val context: Context,
    private val bentoReportGenerator: BentoReportGenerator,
    private val documentExporter: DocumentExporter
) : TaxExporter {

    override suspend fun exportBentoReport(
        invoices: List<Invoice>,
        receipts: List<ReceiptItem>
    ): TaxExportOutcome {
        return try {
        val reportData = TaxExportSupport.buildBentoReportData(invoices, receipts)
        val displayName = InvoiceHammerExportNames.bentoReportPdf()
        val workingDir = File(context.filesDir, "vault/reports").apply { mkdirs() }
        val workingFile = File(workingDir, displayName)

        if (!bentoReportGenerator.generate(reportData, workingFile.absolutePath)) {
            TaxExportOutcome.Failure(FailureMessage("Failed to generate Bento PDF"))
        } else {
        val published = TaxExportSupport.publishToDocuments(
            documentExporter = documentExporter,
            sourcePath = workingFile.absolutePath,
            category = DocumentCategory.Reports,
            displayName = displayName,
            logTag = "ANDROID_TAX_EXPORTER"
        )
        TaxExportOutcome.Success(
            path = published?.shareablePath ?: workingFile.absolutePath,
            savedTo = published?.userVisiblePath
        )
        }
        } catch (e: Exception) {
            TaxExportOutcome.Failure(
                FailureMessage(e.message ?: "Failed to generate report summary")
            )
        }
    }

    override suspend fun exportFullTaxBundle(
        invoices: List<Invoice>,
        receipts: List<ReceiptItem>
    ): TaxExportOutcome = try {
        val displayName = InvoiceHammerExportNames.taxBundleZip()
        val workingDir = File(context.filesDir, "vault/tax_bundles").apply { mkdirs() }
        val cacheFile = File(workingDir, displayName)

        val reportData = TaxExportSupport.buildBentoReportData(invoices, receipts)
        val bentoWorking = File(context.filesDir, "vault/reports").apply { mkdirs() }
        val bentoPath = File(bentoWorking, "bundle_${displayName.removeSuffix(".zip")}.pdf").absolutePath
        val bentoGenerated = bentoReportGenerator.generate(reportData, bentoPath)

        writeTaxBundleZip(
            outputPath = cacheFile.absolutePath,
            businessReportPath = bentoPath.takeIf { bentoGenerated },
            invoicePdfPaths = TaxExportSupport.invoicePdfPaths(invoices)
        )

        val published = TaxExportSupport.publishToDocuments(
            documentExporter = documentExporter,
            sourcePath = cacheFile.absolutePath,
            category = DocumentCategory.TaxBundles,
            displayName = displayName,
            logTag = "ANDROID_TAX_EXPORTER"
        )
        TaxExportOutcome.Success(
            path = published?.shareablePath ?: cacheFile.absolutePath,
            savedTo = published?.userVisiblePath
        )
    } catch (e: Exception) {
        TaxExportOutcome.Failure(
            FailureMessage(e.message ?: "Failed to assemble tax bundle zip")
        )
    }
}
