package com.fordham.toolbelt

import app.cash.turbine.test
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.ClientRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.JobNoteRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.usecase.GenerateTaxReportUseCase
import com.fordham.toolbelt.domain.usecase.GetBusinessStatsUseCase
import com.fordham.toolbelt.ui.viewmodel.StatsViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val getBusinessStatsUseCase: GetBusinessStatsUseCase = mockk(relaxed = true)
    private val generateTaxReportUseCase: GenerateTaxReportUseCase = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val invoiceRepository: InvoiceRepository = mockk(relaxed = true)
    private val receiptRepository: ReceiptRepository = mockk(relaxed = true)
    private val jobNoteRepository: JobNoteRepository = mockk(relaxed = true)
    private val clientRepository: ClientRepository = mockk(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: StatsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { settingsRepository.businessSettingsFlow } returns flowOf(BusinessSettings())
        every { getBusinessStatsUseCase() } returns flowOf(BusinessStats())

        viewModel = StatsViewModel(
            getBusinessStatsUseCase = getBusinessStatsUseCase,
            generateTaxReportUseCase = generateTaxReportUseCase,
            settingsRepository = settingsRepository,
            invoiceRepository = invoiceRepository,
            receiptRepository = receiptRepository,
            jobNoteRepository = jobNoteRepository,
            clientRepository = clientRepository,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `togglePremium saves updated settings`() = runTest {
        val originalSettings = BusinessSettings(isPremium = false)
        every { settingsRepository.businessSettingsFlow } returns flowOf(originalSettings)
        
        // Rebuild viewModel to pick up updated flow initial value
        viewModel = StatsViewModel(
            getBusinessStatsUseCase = getBusinessStatsUseCase,
            generateTaxReportUseCase = generateTaxReportUseCase,
            settingsRepository = settingsRepository,
            invoiceRepository = invoiceRepository,
            receiptRepository = receiptRepository,
            jobNoteRepository = jobNoteRepository,
            clientRepository = clientRepository,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        coEvery { settingsRepository.saveBusinessSettings(any()) } returns SettingsOutcome.Success

        viewModel.togglePremium()

        coVerify {
            settingsRepository.saveBusinessSettings(withArg {
                assertTrue(it.isPremium)
            })
        }
    }

    @Test
    fun `eraseAllInvoices deletes all data from all repositories`() = runTest {
        coEvery { invoiceRepository.deleteAllInvoices() } returns InvoiceOutcome.Success
        coEvery { receiptRepository.deleteAllItems() } returns ReceiptOutcome.Success
        coEvery { jobNoteRepository.deleteAllNotes() } returns JobNoteOutcome.Success
        coEvery { clientRepository.replaceAllClients(any()) } returns ClientOutcome.Success

        viewModel.eraseAllInvoices()

        coVerify {
            invoiceRepository.deleteAllInvoices()
            receiptRepository.deleteAllItems()
            jobNoteRepository.deleteAllNotes()
            clientRepository.replaceAllClients(emptyList())
        }
    }

    @Test
    fun `createStressTestInvoices creates exactly 1000 invoices with matched scenarios`() = runTest {
        val capturedInvoicesLists = mutableListOf<List<Invoice>>()
        val capturedReceipts = mutableListOf<ReceiptItem>()
        val capturedNotes = mutableListOf<JobNote>()

        coEvery { invoiceRepository.insertInvoices(capture(capturedInvoicesLists)) } returns InvoiceOutcome.Success
        coEvery { receiptRepository.insertItem(capture(capturedReceipts)) } returns ReceiptOutcome.Success
        coEvery { jobNoteRepository.insertNote(capture(capturedNotes)) } returns JobNoteOutcome.Success

        viewModel.createStressTestInvoices()

        val capturedInvoices = capturedInvoicesLists.flatten()

        // Verify total count of generated invoices
        assertEquals(1000, capturedInvoices.size)

        // For each of the 100 'i % 10 == 0' cases, one system budget note is generated,
        // and one additional scenario note is generated. So 200 notes total.
        assertEquals(200, capturedNotes.size)

        // For each of the 100 'i % 10 == 0' cases, one receipt item is generated.
        assertEquals(100, capturedReceipts.size)

        // Split profiles:
        // i % 10 == 0:
        // - Profile B (i % 20 == 0): i is a multiple of 20. There are 50 such numbers in 1..1000 (20, 40, ..., 1000).
        // - Profile C (i % 30 == 0): i is a multiple of 30, but not a multiple of 20 (odd multiples of 30: 30, 90, 150, ..., 990).
        //   Let's check odd multiples of 30: 30, 90, 150, 210, 270, 330, 390, 450, 510, 570, 630, 690, 750, 810, 870, 930, 990.
        //   There are 17 such numbers.
        // - Profile A (otherwise): 100 - 50 - 17 = 33 cases.
        
        val systemBudgetNotes = capturedNotes.filter { it.text.startsWith("[SYSTEM_BUDGET]") }
        assertEquals(100, systemBudgetNotes.size)

        val profileBNotes = capturedNotes.filter { it.text.contains("No scope creep.") }
        assertEquals(50, profileBNotes.size)

        val profileCNotes = capturedNotes.filter { it.text.contains("Owner asked to patch two extra walls") }
        assertEquals(17, profileCNotes.size)

        val profileANotes = capturedNotes.filter { it.text.contains("Standard install completed.") }
        assertEquals(33, profileANotes.size)

        // Check Profile B material overrun calculation (budgeted * 1.25)
        val profileBReceipts = capturedReceipts.filter { it.description.contains("Overrun") }
        assertEquals(50, profileBReceipts.size)
        profileBReceipts.forEach { receipt ->
            val correspondingInvoice = capturedInvoices.find { it.id == receipt.linkedInvoiceId }
            assertNotNull(correspondingInvoice)
            val expectedBudget = correspondingInvoice!!.totalAmount * 0.3
            val expectedOverrun = expectedBudget * 1.25
            assertEquals(expectedOverrun, receipt.totalPrice, 0.001)
        }
    }
}
