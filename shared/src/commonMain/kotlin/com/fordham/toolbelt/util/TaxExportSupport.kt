package com.fordham.toolbelt.util

import com.fordham.toolbelt.domain.model.BentoReportData
import com.fordham.toolbelt.domain.model.DocumentCategory
import com.fordham.toolbelt.domain.model.DocumentExportOutcome
import com.fordham.toolbelt.domain.model.DocumentLocation
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.repository.DocumentExporter

internal object TaxExportSupport {
    private const val TAG = "TaxExportSupport"
    fun buildBentoReportData(
        invoices: List<Invoice>,
        receipts: List<ReceiptItem>
    ): BentoReportData {
        val paidInvoices = invoices.filter { it.isPaid && !it.isEstimate }
        val totalIncome = paidInvoices.sumOf { it.totalAmount.value }
        val totalExpenses = receipts.sumOf { it.totalPrice }
        return BentoReportData(
            netProfit = totalIncome - totalExpenses,
            grossIncome = totalIncome,
            expenses = totalExpenses,
            invoices = paidInvoices,
            receiptCount = receipts.size
        )
    }

    suspend fun publishToDocuments(
        documentExporter: DocumentExporter,
        sourcePath: String,
        category: DocumentCategory,
        displayName: String,
        logTag: String
    ): DocumentLocation? {
        return when (
            val outcome = documentExporter.publish(
                sourcePath = sourcePath,
                category = category,
                displayName = displayName
            )
        ) {
            is DocumentExportOutcome.Success -> outcome.location
            is DocumentExportOutcome.Failure -> {
                AppLogger.e(TAG, "$logTag: publish to ${category.subdirectoryName} failed: ${outcome.error.value}")
                null
            }
        }
    }

    fun invoicePdfPaths(invoices: List<Invoice>): List<String> =
        invoices
            .filter { it.pdfPath.value.isNotBlank() }
            .distinctBy { it.pdfPath.value }
            .map { it.pdfPath.value }
}
