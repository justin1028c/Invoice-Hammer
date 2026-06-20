package com.fordham.toolbelt

import app.cash.turbine.test
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoiceOutcome
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.ui.viewmodel.HistoryViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private lateinit var viewModel: HistoryViewModel
    private val invoiceRepository: InvoiceRepository = mockk(relaxed = true)
    private val composeAiInvoiceReminderUseCase: com.fordham.toolbelt.domain.usecase.ComposeAiInvoiceReminderUseCase = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testInvoices = listOf(
        Invoice(
            id = com.fordham.toolbelt.domain.model.InvoiceId("1"), clientName = "John Smith", clientAddress = "123 Main St",
            date = "Jan 01, 2026", totalAmount = 1500.0, itemsSummary = "Drywall, Painting",
            isPaid = true, isEstimate = false
        ),
        Invoice(
            id = com.fordham.toolbelt.domain.model.InvoiceId("2"), clientName = "Jane Doe", clientAddress = "456 Oak Ave",
            date = "Feb 15, 2026", totalAmount = 2500.0, itemsSummary = "Roofing",
            isPaid = false, isEstimate = false
        ),
        Invoice(
            id = com.fordham.toolbelt.domain.model.InvoiceId("3"), clientName = "Bob Builder", clientAddress = "789 Pine Rd",
            date = "Mar 01, 2026", totalAmount = 800.0, itemsSummary = "Plumbing",
            isPaid = false, isEstimate = true
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { invoiceRepository.allInvoices } returns flowOf(testInvoices)
        viewModel = HistoryViewModel(invoiceRepository, composeAiInvoiceReminderUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial ui state is correct`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.invoiceToDelete)
            assertEquals("", state.searchQuery)
            assertFalse(state.showPaidOnly)
        }
    }

    @Test
    fun `filteredInvoices returns all when no filter`() = runTest {
        viewModel.filteredInvoices.test {
            val invoices = awaitItem()
            assertEquals(3, invoices.size)
        }
    }

    @Test
    fun `filteredInvoices filters by search query on client name`() = runTest {
        viewModel.onSearchQueryChange("John")

        viewModel.filteredInvoices.test {
            val invoices = awaitItem()
            assertEquals(1, invoices.size)
            assertEquals("John Smith", invoices[0].clientName)
        }
    }

    @Test
    fun `filteredInvoices filters by search query on items summary`() = runTest {
        viewModel.onSearchQueryChange("Roofing")

        viewModel.filteredInvoices.test {
            val invoices = awaitItem()
            assertEquals(1, invoices.size)
            assertEquals("Jane Doe", invoices[0].clientName)
        }
    }

    @Test
    fun `filteredInvoices filters paid only`() = runTest {
        viewModel.onShowPaidOnlyChange(true)

        viewModel.filteredInvoices.test {
            val invoices = awaitItem()
            assertEquals(1, invoices.size)
            assertTrue(invoices[0].isPaid)
            assertEquals("John Smith", invoices[0].clientName)
        }
    }

    @Test
    fun `filteredInvoices combines search and paid filter`() = runTest {
        viewModel.onSearchQueryChange("Jane")
        viewModel.onShowPaidOnlyChange(true)

        viewModel.filteredInvoices.test {
            val invoices = awaitItem()
            // Jane is not paid, so empty
            assertTrue(invoices.isEmpty())
        }
    }

    @Test
    fun `deleteInvoice calls repository`() = runTest {
        coEvery { invoiceRepository.deleteInvoice(any()) } returns InvoiceOutcome.Success

        viewModel.deleteInvoice(testInvoices[0])

        coVerify { invoiceRepository.deleteInvoice(testInvoices[0]) }
    }

    @Test
    fun `markInvoicePaid updates invoice with isPaid true`() = runTest {
        coEvery { invoiceRepository.updateInvoice(any()) } returns InvoiceOutcome.Success

        viewModel.markInvoicePaid(testInvoices[1])

        coVerify { invoiceRepository.updateInvoice(testInvoices[1].copy(isPaid = true)) }
    }

    @Test
    fun `convertEstimateToInvoice sets isEstimate false`() = runTest {
        coEvery { invoiceRepository.updateInvoice(any()) } returns InvoiceOutcome.Success

        viewModel.convertEstimateToInvoice(testInvoices[2])

        coVerify { invoiceRepository.updateInvoice(testInvoices[2].copy(isEstimate = false)) }
    }
}
