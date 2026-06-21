package com.fordham.toolbelt.domain.usecase

import app.cash.turbine.test
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GetBusinessStatsUseCaseTest {

    private val invoiceRepository: InvoiceRepository = mockk()
    private val receiptRepository: ReceiptRepository = mockk()
    private lateinit var useCase: GetBusinessStatsUseCase

    private val paidInvoices = listOf(
        Invoice(id = com.fordham.toolbelt.domain.model.InvoiceId("1"), clientName = ClientName("Client A"), clientAddress = ClientAddress(""), date = "Jan 01, 2026",
            totalAmount = MoneyAmount(3000.0), itemsSummary = ItemsSummary("Drywall"), isPaid = true, isEstimate = false),
        Invoice(id = com.fordham.toolbelt.domain.model.InvoiceId("2"), clientName = ClientName("Client A"), clientAddress = ClientAddress(""), date = "Feb 01, 2026",
            totalAmount = MoneyAmount(2000.0), itemsSummary = ItemsSummary("Painting"), isPaid = true, isEstimate = false),
        Invoice(id = com.fordham.toolbelt.domain.model.InvoiceId("3"), clientName = ClientName("Client B"), clientAddress = ClientAddress(""), date = "Mar 01, 2026",
            totalAmount = MoneyAmount(1500.0), itemsSummary = ItemsSummary("Plumbing"), isPaid = true, isEstimate = false)
    )

    private val unpaidInvoice = Invoice(
        id = com.fordham.toolbelt.domain.model.InvoiceId("4"), clientName = ClientName("Client C"), clientAddress = ClientAddress(""), date = "Apr 01, 2026",
        totalAmount = MoneyAmount(1000.0), itemsSummary = ItemsSummary("Electrical"), isPaid = false, isEstimate = false
    )

    private val estimate = Invoice(
        id = com.fordham.toolbelt.domain.model.InvoiceId("5"), clientName = ClientName("Client D"), clientAddress = ClientAddress(""), date = "Apr 15, 2026",
        totalAmount = MoneyAmount(5000.0), itemsSummary = ItemsSummary("Roofing"), isPaid = true, isEstimate = true
    )

    private val receipts = listOf(
        ReceiptItem(id = com.fordham.toolbelt.domain.model.ReceiptId("r1"), description = "Lumber", quantity = 1.0, unitPrice = 500.0,
            totalPrice = 500.0, clientName = "Client A", isBilled = true),
        ReceiptItem(id = com.fordham.toolbelt.domain.model.ReceiptId("r2"), description = "Nails", quantity = 2.0, unitPrice = 25.0,
            totalPrice = 50.0, clientName = "Client A", isBilled = true),
        ReceiptItem(id = com.fordham.toolbelt.domain.model.ReceiptId("r3"), description = "Pipe", quantity = 1.0, unitPrice = 200.0,
            totalPrice = 200.0, clientName = "Client B", isBilled = false),
        ReceiptItem(id = com.fordham.toolbelt.domain.model.ReceiptId("r4"), description = "Wire", quantity = 1.0, unitPrice = 100.0,
            totalPrice = 100.0, clientName = "General", isBilled = false)
    )

    @Before
    fun setup() {
        val allInvoices = paidInvoices + unpaidInvoice + estimate
        every { invoiceRepository.allInvoices } returns flowOf(allInvoices)
        every { receiptRepository.allItems } returns flowOf(ReceiptListOutcome.Success(receipts))
        useCase = GetBusinessStatsUseCase(invoiceRepository, receiptRepository)
    }

    @Test
    fun `calculates net profit from paid non-estimate invoices minus expenses`() = runTest {
        useCase().test {
            val stats = awaitItem()
            // Revenue = 3000 + 2000 + 1500 = 6500 (only paid, non-estimate)
            // Expenses = 500 + 50 + 200 + 100 = 850
            // Net profit = 6500 - 850 = 5650
            assertEquals(5650.0, stats.netProfit, 0.01)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `calculates total expenses`() = runTest {
        useCase().test {
            val stats = awaitItem()
            assertEquals(850.0, stats.totalExpenses, 0.01)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `calculates unbilled expenses`() = runTest {
        useCase().test {
            val stats = awaitItem()
            // Unbilled = Pipe (200) + Wire (100) = 300
            assertEquals(300.0, stats.unbilledExpenses, 0.01)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `excludes estimates from revenue`() = runTest {
        useCase().test {
            val stats = awaitItem()
            // Estimate for $5000 should NOT be in revenue
            // Revenue should be 6500, not 11500
            assertTrue(stats.netProfit < 7000)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `excludes unpaid invoices from revenue`() = runTest {
        useCase().test {
            val stats = awaitItem()
            // Unpaid $1000 should NOT be in revenue
            // Revenue should be 6500, not 7500
            assertTrue(stats.netProfit < 7000)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `generates per-project stats`() = runTest {
        useCase().test {
            val stats = awaitItem()
            val projectA = stats.projectStats.find { it.clientName == "Client A" }
            assertNotNull(projectA)
            // Client A revenue = 3000 + 2000 = 5000
            assertEquals(5000.0, projectA!!.revenue, 0.01)
            // Client A expenses = 500 + 50 = 550
            assertEquals(550.0, projectA.expenses, 0.01)
            assertEquals(4450.0, projectA.profit, 0.01)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `calculates profit margin`() = runTest {
        useCase().test {
            val stats = awaitItem()
            // Margin = (5650 / 6500) * 100 = ~86%
            assertEquals(86, stats.profitMargin)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `handles empty data`() = runTest {
        every { invoiceRepository.allInvoices } returns flowOf(emptyList())
        every { receiptRepository.allItems } returns flowOf(ReceiptListOutcome.Success(emptyList()))
        val emptyUseCase = GetBusinessStatsUseCase(invoiceRepository, receiptRepository)

        emptyUseCase().test {
            val stats = awaitItem()
            assertEquals(0.0, stats.netProfit, 0.01)
            assertEquals(0.0, stats.totalExpenses, 0.01)
            assertEquals(0, stats.profitMargin)
            assertTrue(stats.projectStats.isEmpty())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `calculates YTD metrics aging receivables and tax projection`() = runTest {
        val today = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        val currentYear = today.year

        // Create invoices dynamically in the current year
        val invoicePaidThisYear = Invoice(
            id = com.fordham.toolbelt.domain.model.InvoiceId("ytd-1"),
            clientName = ClientName("Client YTD"),
            clientAddress = ClientAddress(""),
            date = "01/01/$currentYear",
            totalAmount = MoneyAmount(4000.0),
            itemsSummary = ItemsSummary("Drywall YTD"),
            isPaid = true,
            isEstimate = false
        )

        // Create an unpaid invoice to verify aging receivables
        // Let's make it 10 days old relative to today
        val date10DaysAgo = today.toEpochDays() - 10
        val localDate10DaysAgo = kotlinx.datetime.LocalDate.fromEpochDays(date10DaysAgo)
        val month10DaysAgo = localDate10DaysAgo.monthNumber
        val day10DaysAgo = localDate10DaysAgo.dayOfMonth
        val year10DaysAgo = localDate10DaysAgo.year
        // format as M/D/YYYY
        val dateStr10DaysAgo = "$month10DaysAgo/$day10DaysAgo/$year10DaysAgo"

        val unpaidInvoice0to30 = Invoice(
            id = com.fordham.toolbelt.domain.model.InvoiceId("unpaid-0-30"),
            clientName = ClientName("Client Unpaid 1"),
            clientAddress = ClientAddress(""),
            date = dateStr10DaysAgo,
            totalAmount = MoneyAmount(1500.0),
            itemsSummary = ItemsSummary("Labor"),
            isPaid = false,
            isEstimate = false
        )

        // Let's make another unpaid invoice 40 days old relative to today
        val date40DaysAgo = today.toEpochDays() - 40
        val localDate40DaysAgo = kotlinx.datetime.LocalDate.fromEpochDays(date40DaysAgo)
        val month40DaysAgo = localDate40DaysAgo.monthNumber
        val day40DaysAgo = localDate40DaysAgo.dayOfMonth
        val year40DaysAgo = localDate40DaysAgo.year
        val dateStr40DaysAgo = "$month40DaysAgo/$day40DaysAgo/$year40DaysAgo"

        val unpaidInvoice31to60 = Invoice(
            id = com.fordham.toolbelt.domain.model.InvoiceId("unpaid-31-60"),
            clientName = ClientName("Client Unpaid 2"),
            clientAddress = ClientAddress(""),
            date = dateStr40DaysAgo,
            totalAmount = MoneyAmount(2500.0),
            itemsSummary = ItemsSummary("Materials"),
            isPaid = false,
            isEstimate = false
        )

        // Let's make another unpaid invoice 70 days old relative to today
        val date70DaysAgo = today.toEpochDays() - 70
        val localDate70DaysAgo = kotlinx.datetime.LocalDate.fromEpochDays(date70DaysAgo)
        val month70DaysAgo = localDate70DaysAgo.monthNumber
        val day70DaysAgo = localDate70DaysAgo.dayOfMonth
        val year70DaysAgo = localDate70DaysAgo.year
        val dateStr70DaysAgo = "$month70DaysAgo/$day70DaysAgo/$year70DaysAgo"

        val unpaidInvoice61Plus = Invoice(
            id = com.fordham.toolbelt.domain.model.InvoiceId("unpaid-61-plus"),
            clientName = ClientName("Client Unpaid 3"),
            clientAddress = ClientAddress(""),
            date = dateStr70DaysAgo,
            totalAmount = MoneyAmount(3500.0),
            itemsSummary = ItemsSummary("Inspection"),
            isPaid = false,
            isEstimate = false
        )

        // Mock receipts logged this year
        val receiptThisYear = ReceiptItem(
            id = com.fordham.toolbelt.domain.model.ReceiptId("r-ytd"),
            description = "Tool Purchase",
            quantity = 1.0,
            unitPrice = 1000.0,
            totalPrice = 1000.0,
            clientName = "Client YTD",
            isBilled = false,
            lastUpdated = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        )

        val invoicesList = listOf(invoicePaidThisYear, unpaidInvoice0to30, unpaidInvoice31to60, unpaidInvoice61Plus)
        val receiptsList = listOf(receiptThisYear)

        every { invoiceRepository.allInvoices } returns flowOf(invoicesList)
        every { receiptRepository.allItems } returns flowOf(ReceiptListOutcome.Success(receiptsList))

        val testUseCase = GetBusinessStatsUseCase(invoiceRepository, receiptRepository)
        testUseCase().test {
            val stats = awaitItem()
            // YTD Revenue: 4000.0 (from invoicePaidThisYear)
            // YTD Expenses: 1000.0 (from receiptThisYear)
            // YTD Net Profit: 3000.0
            assertEquals(4000.0, stats.ytdRevenue, 0.01)
            assertEquals(1000.0, stats.ytdExpenses, 0.01)
            assertEquals(3000.0, stats.ytdNetProfit, 0.01)

            // Projected tax: 3000.0 * 20% = 600.0
            assertEquals(600.0, stats.projectedTax, 0.01)

            // Current Quarter should be between 1 and 4
            assertTrue(stats.currentQuarter in 1..4)

            // Aging Receivables:
            // Total Outstanding = 1500 + 2500 + 3500 = 7500
            // 0 to 30: 1500
            // 31 to 60: 2500
            // 61+: 3500
            assertEquals(7500.0, stats.totalOutstanding, 0.01)
            assertEquals(1500.0, stats.outstanding0to30, 0.01)
            assertEquals(2500.0, stats.outstanding31to60, 0.01)
            assertEquals(3500.0, stats.outstanding61Plus, 0.01)

            cancelAndConsumeRemainingEvents()
        }
    }
}
