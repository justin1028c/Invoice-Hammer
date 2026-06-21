package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.util.DateTimeUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class FinancialSummary(
    val revenue: Double,
    val expenses: Double,
    val profit: Double
) {
    val formattedRevenue: String get() = DateTimeUtil.formatMoney(revenue)
    val formattedExpenses: String get() = DateTimeUtil.formatMoney(expenses)
    val formattedProfit: String get() = DateTimeUtil.formatMoney(profit)
}

class GetClientFinancialSummaryUseCase(
    private val invoiceRepository: InvoiceRepository,
    private val receiptRepository: ReceiptRepository
) {
    operator fun invoke(clientName: String): Flow<FinancialSummary> {
        return combine(
            invoiceRepository.allInvoices,
            receiptRepository.allItems
        ) { invoices, receiptsResult ->
            val revenue = invoices
                .filter { it.clientName.value == clientName && it.isPaid && !it.isEstimate }
                .sumOf { it.totalAmount.value }

            val expenses = if (receiptsResult is ReceiptListOutcome.Success) {
                receiptsResult.receipts
                    .filter { it.clientName == clientName }
                    .sumOf { it.totalPrice }
            } else 0.0
            
            val profit = revenue - expenses
            FinancialSummary(revenue, expenses, profit)
        }
    }
}