package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.BusinessStats
import com.fordham.toolbelt.domain.model.ProjectStat
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


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
            val revenue = paidNonEstimateInvoices.sumOf { it.totalAmount.value }
            val expenses = receipts.sumOf { it.totalPrice }
            val netProfit = revenue - expenses
            val profitMargin = if (revenue > 0.0) ((netProfit / revenue) * 100.0).toInt() else 0

            val totalDurationSeconds = paidNonEstimateInvoices.sumOf { it.durationSeconds.value }
            val unbilledExpenses = receipts.filter { !it.isBilled }.sumOf { it.totalPrice }

            // Client-Specific Project Stats
            val allClients = (invoices.map { it.clientName.value } + receipts.map { it.clientName })
                .filter { it.isNotBlank() }
                .distinct()

            val projectStats = allClients.map { client ->
                val clientInvoices = nonEstimateInvoices.filter { it.clientName.value == client }
                val clientPaidInvoices = clientInvoices.filter { it.isPaid }
                val clientReceipts = receipts.filter { it.clientName == client }

                val clientRevenue = clientPaidInvoices.sumOf { it.totalAmount.value }
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

            // Year-to-Date Calculations
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val currentYear = today.year

            val ytdPaidInvoices = paidNonEstimateInvoices.filter {
                val invDate = com.fordham.toolbelt.util.DateTimeUtil.parseDate(it.date)
                val year = invDate?.year ?: Instant.fromEpochMilliseconds(it.lastUpdated).toLocalDateTime(TimeZone.currentSystemDefault()).year
                year == currentYear
            }
            val ytdRevenue = ytdPaidInvoices.sumOf { it.totalAmount.value }

            val ytdReceipts = receipts.filter {
                val year = Instant.fromEpochMilliseconds(it.lastUpdated).toLocalDateTime(TimeZone.currentSystemDefault()).year
                year == currentYear
            }
            val ytdExpenses = ytdReceipts.sumOf { it.totalPrice }
            val ytdNetProfit = ytdRevenue - ytdExpenses

            // Outstanding Invoices & aging buckets
            val unpaidNonEstimateInvoices = invoices.filter { !it.isPaid && !it.isEstimate }
            val totalOutstanding = unpaidNonEstimateInvoices.sumOf { it.totalAmount.value }

            var outstanding0to30 = 0.0
            var outstanding31to60 = 0.0
            var outstanding61Plus = 0.0

            unpaidNonEstimateInvoices.forEach { invoice ->
                val invDate = com.fordham.toolbelt.util.DateTimeUtil.parseDate(invoice.date)
                val ageDays = if (invDate != null) {
                    (today.toEpochDays() - invDate.toEpochDays()).coerceAtLeast(0)
                } else {
                    val fallbackDate = Instant.fromEpochMilliseconds(invoice.lastUpdated).toLocalDateTime(TimeZone.currentSystemDefault()).date
                    (today.toEpochDays() - fallbackDate.toEpochDays()).coerceAtLeast(0)
                }

                when {
                    ageDays <= 30 -> outstanding0to30 += invoice.totalAmount.value
                    ageDays <= 60 -> outstanding31to60 += invoice.totalAmount.value
                    else -> outstanding61Plus += invoice.totalAmount.value
                }
            }

            // Quarterly Estimated Tax
            val projectedTax = maxOf(0.0, ytdNetProfit * 0.20)
            val currentQuarter = when (today.monthNumber) {
                in 1..3 -> 1
                in 4..6 -> 2
                in 7..9 -> 3
                else -> 4
            }

            BusinessStats(
                netProfit = netProfit,
                totalExpenses = expenses,
                totalDurationSeconds = totalDurationSeconds,
                unbilledExpenses = unbilledExpenses,
                projectStats = projectStats,
                profitMargin = profitMargin,
                ytdRevenue = ytdRevenue,
                ytdExpenses = ytdExpenses,
                ytdNetProfit = ytdNetProfit,
                totalOutstanding = totalOutstanding,
                outstanding0to30 = outstanding0to30,
                outstanding31to60 = outstanding31to60,
                outstanding61Plus = outstanding61Plus,
                projectedTax = projectedTax,
                currentQuarter = currentQuarter
            )
        }
    }
}

