package com.fordham.toolbelt.util

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.model.TaxExportOutcome

class IosTaxExporter : TaxExporter {
    
    override suspend fun exportBentoReport(
        invoices: List<Invoice>,
        receipts: List<ReceiptItem>
    ): TaxExportOutcome {
        // Concrete bridge stub for iOS Bento Report Generation
        return TaxExportOutcome.Failure(
            com.fordham.toolbelt.domain.model.FailureMessage("Tax Bento PDF report generation is not currently supported on iOS.")
        )
    }

    override suspend fun exportFullTaxBundle(
        invoices: List<Invoice>,
        receipts: List<ReceiptItem>
    ): TaxExportOutcome {
        // Concrete bridge stub for iOS Full ZIP Tax Bundle Generation
        return TaxExportOutcome.Failure(
            com.fordham.toolbelt.domain.model.FailureMessage("ZIP Tax bundle export is not currently supported on iOS.")
        )
    }
}
