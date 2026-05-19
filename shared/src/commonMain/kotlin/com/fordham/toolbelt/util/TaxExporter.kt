package com.fordham.toolbelt.util

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.model.TaxExportOutcome

interface TaxExporter {
    suspend fun exportBentoReport(invoices: List<Invoice>, receipts: List<ReceiptItem>): TaxExportOutcome
    suspend fun exportFullTaxBundle(invoices: List<Invoice>, receipts: List<ReceiptItem>): TaxExportOutcome
}
