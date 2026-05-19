package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.BusinessStats
import com.fordham.toolbelt.domain.model.ProjectStat
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetBusinessStatsUseCase(
    private val invoiceRepository: InvoiceRepository,
    private val receiptRepository: ReceiptRepository
) {
    operator fun invoke(): Flow<BusinessStats> {
        return combine(
            invoiceRepository.allInvoices,
            receiptRepository.allItems
        ) { invoices, receiptsResult ->
            val receipts = if (receiptsResult is ReceiptListOutcome.Success) {
                receiptsResult.receipts
            } else {
                emptyList()
            }

            // Core filters
            val paidNonEstimateInvoices = invoices.filter { it.isPaid && !it.isEstimate }
            val nonEstimateInvoices = invoices.filter { !it.isEstimate }

            // Global Calculations
            val revenue = paidNonEstimateInvoices.sumOf { it.totalAmount }
            val expenses = receipts.sumOf { it.totalPrice }
            val netProfit = revenue - expenses
            val profitMargin = if (revenue > 0.0) ((netProfit / revenue) * 100.0).toInt() else 0

            val totalDurationSeconds = paidNonEstimateInvoices.sumOf { it.durationSeconds }
            val unbilledExpenses = receipts.filter { !it.isBilled }.sumOf { it.totalPrice }

            // Client-Specific Project Stats
            val allClients = (invoices.map { it.clientName } + receipts.map { it.clientName })
                .filter { it.isNotBlank() }
                .distinct()

            val projectStats = allClients.map { client ->
                val clientInvoices = nonEstimateInvoices.filter { it.clientName == client }
                val clientPaidInvoices = clientInvoices.filter { it.isPaid }
                val clientReceipts = receipts.filter { it.clientName == client }

                val clientRevenue = clientPaidInvoices.sumOf { it.totalAmount }
                val clientExpenses = clientReceipts.sumOf { it.totalPrice }
                val clientProfit = clientRevenue - clientExpenses

                val progress = if (clientInvoices.isNotEmpty()) {
                    (clientPaidInvoices.size.toDouble() / clientInvoices.size.toDouble()).toFloat()
                } else {
                    0.0f
                }

                ProjectStat(
                    clientName = client,
                    revenue = clientRevenue,
                    expenses = clientExpenses,
                    profit = clientProfit,
                    progress = progress
                )
            }.sortedByDescending { it.revenue }

            BusinessStats(
                netProfit = netProfit,
                totalExpenses = expenses,
                totalDurationSeconds = totalDurationSeconds,
                unbilledExpenses = unbilledExpenses,
                projectStats = projectStats,
                profitMargin = profitMargin
            )
        }
    }
}

