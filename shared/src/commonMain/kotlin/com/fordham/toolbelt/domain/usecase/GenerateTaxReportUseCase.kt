package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.util.TaxExporter
import kotlinx.coroutines.flow.first

class GenerateTaxReportUseCase(
    private val invoiceRepository: InvoiceRepository,
    private val receiptRepository: ReceiptRepository,
    private val settingsRepository: SettingsRepository,
    private val taxExporter: TaxExporter
) {
    suspend fun executeBentoReport(): TaxExportOutcome {
        val settings = settingsRepository.businessSettingsFlow.first()
        if (!settings.isPremium) {
            return TaxExportOutcome.Failure(FailureMessage("Premium subscription required"))
        }

        val invoices = invoiceRepository.allInvoices.first()
        
        val receiptsResult = receiptRepository.allItems.first()
        val receipts = when (receiptsResult) {
            is ReceiptListOutcome.Success -> receiptsResult.receipts
            is ReceiptListOutcome.Failure -> {
                return TaxExportOutcome.Failure(receiptsResult.error)
            }
        }

        return taxExporter.exportBentoReport(invoices, receipts)
    }

    suspend fun executeZip(): TaxExportOutcome {
        val settings = settingsRepository.businessSettingsFlow.first()
        if (!settings.isPremium) {
            return TaxExportOutcome.Failure(FailureMessage("Premium subscription required"))
        }

        val invoices = invoiceRepository.allInvoices.first()
        
        val receiptsResult = receiptRepository.allItems.first()
        val receipts = when (receiptsResult) {
            is ReceiptListOutcome.Success -> receiptsResult.receipts
            is ReceiptListOutcome.Failure -> {
                return TaxExportOutcome.Failure(receiptsResult.error)
            }
        }

        return taxExporter.exportFullTaxBundle(invoices, receipts)
    }
}
