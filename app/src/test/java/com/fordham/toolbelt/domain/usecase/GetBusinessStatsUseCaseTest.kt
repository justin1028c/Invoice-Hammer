package com.fordham.toolbelt.domain.usecase

import app.cash.turbine.test
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GetBusinessStatsUseCaseTest {

    private val invoiceRepository: InvoiceRepository = mockk()
    private val receiptRepository: ReceiptRepository = mockk()
    private lateinit var useCase: GetBusinessStatsUseCase

    private val paidInvoices = listOf(
        Invoice(id = com.fordham.toolbelt.domain.model.InvoiceId("1"), clientName = "Client A", clientAddress = "", date = "Jan 01, 2026",
            totalAmount = 3000.0, itemsSummary = "Drywall", isPaid = true, isEstimate = false),
        Invoice(id = com.fordham.toolbelt.domain.model.InvoiceId("2"), clientName = "Client A", clientAddress = "", date = "Feb 01, 2026",
            totalAmount = 2000.0, itemsSummary = "Painting", isPaid = true, isEstimate = false),
        Invoice(id = com.fordham.toolbelt.domain.model.InvoiceId("3"), clientName = "Client B", clientAddress = "", date = "Mar 01, 2026",
            totalAmount = 1500.0, itemsSummary = "Plumbing", isPaid = true, isEstimate = false)
    )

    private val unpaidInvoice = Invoice(
        id = com.fordham.toolbelt.domain.model.InvoiceId("4"), clientName = "Client C", clientAddress = "", date = "Apr 01, 2026",
        totalAmount = 1000.0, itemsSummary = "Electrical", isPaid = false, isEstimate = false
    )

    private val estimate = Invoice(
        id = com.fordham.toolbelt.domain.model.InvoiceId("5"), clientName = "Client D", clientAddress = "", date = "Apr 15, 2026",
        totalAmount = 5000.0, itemsSummary = "Roofing", isPaid = true, isEstimate = true
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
}
