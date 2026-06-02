package com.fordham.toolbelt.util

import com.fordham.toolbelt.domain.model.DocumentCategory
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.model.TaxExportOutcome
import com.fordham.toolbelt.domain.repository.BentoReportGenerator
import com.fordham.toolbelt.domain.repository.DocumentExporter
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class IosTaxExporter(
    private val documentExporter: DocumentExporter,
    private val bentoReportGenerator: BentoReportGenerator
) : TaxExporter {

    override suspend fun exportBentoReport(
        invoices: List<Invoice>,
        receipts: List<ReceiptItem>
    ): TaxExportOutcome {
        return try {
        val reportData = TaxExportSupport.buildBentoReportData(invoices, receipts)
        val displayName = InvoiceHammerExportNames.bentoReportPdf()
        val workingFile = "${ensureVaultDir("reports")}/$displayName"

        if (!bentoReportGenerator.generate(reportData, workingFile)) {
            TaxExportOutcome.Failure(FailureMessage("Failed to generate Bento PDF"))
        } else {
        val published = TaxExportSupport.publishToDocuments(
            documentExporter = documentExporter,
            sourcePath = workingFile,
            category = DocumentCategory.Reports,
            displayName = displayName,
            logTag = "IOS_TAX_EXPORTER"
        )
        TaxExportOutcome.Success(
            path = published?.shareablePath ?: workingFile,
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
        val cacheFile = "${ensureVaultDir("tax_bundles")}/$displayName"

        val reportData = TaxExportSupport.buildBentoReportData(invoices, receipts)
        val bentoPath = "${ensureVaultDir("reports")}/bundle_${displayName.removeSuffix(".zip")}.pdf"
        val bentoGenerated = bentoReportGenerator.generate(reportData, bentoPath)

        writeTaxBundleZip(
            outputPath = cacheFile,
            businessReportPath = bentoPath.takeIf { bentoGenerated },
            invoicePdfPaths = TaxExportSupport.invoicePdfPaths(invoices)
        )

        val published = TaxExportSupport.publishToDocuments(
            documentExporter = documentExporter,
            sourcePath = cacheFile,
            category = DocumentCategory.TaxBundles,
            displayName = displayName,
            logTag = "IOS_TAX_EXPORTER"
        )
        TaxExportOutcome.Success(
            path = published?.shareablePath ?: cacheFile,
            savedTo = published?.userVisiblePath
        )
    } catch (e: Exception) {
        TaxExportOutcome.Failure(
            FailureMessage(e.message ?: "Failed to assemble tax bundle zip")
        )
    }

    private fun documentsRoot(): String {
        return NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String
            ?: error("iOS Documents directory not resolved")
    }

    private fun ensureVaultDir(subfolder: String): String {
        val dir = "${documentsRoot()}/vault/$subfolder"
        NSFileManager.defaultManager.createDirectoryAtPath(
            dir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
        return dir
    }
}
