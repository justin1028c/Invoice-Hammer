package com.fordham.toolbelt

import app.cash.turbine.test
import com.fordham.toolbelt.domain.repository.DraftRepository
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.usecase.BillLaborUseCase
import com.fordham.toolbelt.domain.usecase.GenerateAndSaveInvoiceUseCase
import com.fordham.toolbelt.domain.usecase.ProcessInvoiceAiUseCase
import com.fordham.toolbelt.domain.usecase.SaveBusinessLogoUseCase
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceIntent
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewInvoiceViewModelTest {

    private lateinit var viewModel: NewInvoiceViewModel
    private val receiptRepository: ReceiptRepository = mockk(relaxed = true)
    private val processInvoiceAiUseCase: ProcessInvoiceAiUseCase = mockk()
    private val billLaborUseCase: BillLaborUseCase = mockk(relaxed = true)
    private val generateAndSaveInvoiceUseCase: GenerateAndSaveInvoiceUseCase = mockk(relaxed = true)
    private val draftRepository: DraftRepository = mockk()
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val saveBusinessLogoUseCase: SaveBusinessLogoUseCase = mockk(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()
    private val draftStateFlow = MutableStateFlow<DraftInvoice>(DraftInvoice())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { draftRepository.getDraft() } returns draftStateFlow
        coEvery { draftRepository.saveDraft(any()) } answers {
            draftStateFlow.value = firstArg()
        }
        coEvery { draftRepository.clearDraft() } answers {
            draftStateFlow.value = DraftInvoice()
        }
        coEvery { receiptRepository.getUnassignedReceipts() } returns flowOf(ReceiptListOutcome.Success(emptyList()))
        every { settingsRepository.businessSettingsFlow } returns flowOf(BusinessSettings())
        coEvery { settingsRepository.getBusinessSettings() } returns BusinessSettings()

        viewModel = NewInvoiceViewModel(
            receiptRepository, processInvoiceAiUseCase, billLaborUseCase,
            generateAndSaveInvoiceUseCase, draftRepository, settingsRepository, saveBusinessLogoUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has correct defaults`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.clientName)
            assertEquals("", state.clientAddress)
            assertEquals("7.0", state.taxText)
            assertTrue(state.lineItems.isEmpty())
            assertFalse(state.isProcessingAi)
            assertFalse(state.canSave)
            assertFalse(state.canAddManual)
        }
    }

    @Test
    fun `onClientNameChange updates state`() = runTest {
        viewModel.onIntent(NewInvoiceIntent.OnClientNameChange("John Smith"))

        viewModel.uiState.test {
            assertEquals("John Smith", awaitItem().clientName)
        }
    }

    @Test
    fun `addManualLineItem clears fields and adds item`() = runTest {
        viewModel.onIntent(NewInvoiceIntent.OnItemDescChange("Drywall patch"))
        viewModel.onIntent(NewInvoiceIntent.OnItemAmtChange("75.50"))

        viewModel.onIntent(NewInvoiceIntent.AddManualLineItem)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.lineItems.size)
            assertEquals("Drywall patch", state.lineItems[0].description)
            assertEquals(75.50, state.lineItems[0].amount, 0.01)
            assertEquals("", state.itemDesc)
            assertEquals("", state.itemAmt)
        }
    }

    @Test
    fun `removeLineItem removes from list`() = runTest {
        val item1 = LineItem("Item 1", 100.0, "Drywall")
        val item2 = LineItem("Item 2", 200.0, "Drywall")
        
        // Add them first
        viewModel.onIntent(NewInvoiceIntent.OnItemDescChange("Item 1"))
        viewModel.onIntent(NewInvoiceIntent.OnItemAmtChange("100.0"))
        viewModel.onIntent(NewInvoiceIntent.AddManualLineItem)

        viewModel.onIntent(NewInvoiceIntent.OnItemDescChange("Item 2"))
        viewModel.onIntent(NewInvoiceIntent.OnItemAmtChange("200.0"))
        viewModel.onIntent(NewInvoiceIntent.AddManualLineItem)

        viewModel.onIntent(NewInvoiceIntent.RemoveLineItem(item1))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.lineItems.size)
            assertEquals("Item 2", state.lineItems[0].description)
        }
    }

    @Test
    fun `canSave requires items and client name`() = runTest {
        // No items, no client -> can't save
        viewModel.uiState.test {
            assertFalse(awaitItem().canSave)
        }

        // Has items but no client -> can't save
        viewModel.onIntent(NewInvoiceIntent.OnItemDescChange("Test"))
        viewModel.onIntent(NewInvoiceIntent.OnItemAmtChange("100.0"))
        viewModel.onIntent(NewInvoiceIntent.AddManualLineItem)
        
        viewModel.uiState.test {
            assertFalse(awaitItem().canSave)
        }

        // Has items AND client -> can save
        viewModel.onIntent(NewInvoiceIntent.OnClientNameChange("Client"))
        viewModel.onIntent(NewInvoiceIntent.OnClientAddressChange("Address"))
        viewModel.uiState.test {
            assertTrue(awaitItem().canSave)
        }
    }

    @Test
    fun `canAddManual requires desc and positive amount`() = runTest {
        viewModel.onIntent(NewInvoiceIntent.OnItemDescChange("Something"))
        viewModel.onIntent(NewInvoiceIntent.OnItemAmtChange("0"))
        viewModel.uiState.test {
            assertFalse(awaitItem().canAddManual)
        }

        viewModel.onIntent(NewInvoiceIntent.OnItemAmtChange("50"))
        viewModel.uiState.test {
            assertTrue(awaitItem().canAddManual)
        }
    }

    @Test
    fun `processInvoiceAi success populates pending items`() = runTest {
        val aiResult = AiInvoiceResult(
            clientName = "AI Client",
            clientAddress = "AI Address",
            items = listOf(LineItem("AI Item", 300.0, "Drywall"))
        )
        coEvery { processInvoiceAiUseCase(any(), any()) } returns InvoiceTextOutcome.Success(aiResult)

        viewModel.onIntent(NewInvoiceIntent.OnItemDescChange("install drywall in bedroom"))
        viewModel.onIntent(NewInvoiceIntent.ProcessInvoiceAi(listOf("Drywall", "Painting")))

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessingAi)
            assertEquals("AI Client", state.clientName)
            assertEquals("AI Address", state.clientAddress)
            assertEquals(1, state.pendingAi.size)
            assertTrue(state.showAiConf)
        }
    }

    @Test
    fun `processInvoiceAi error sets error message`() = runTest {
        coEvery { processInvoiceAiUseCase(any(), any()) } returns InvoiceTextOutcome.Failure(
            FailureMessage("Failed to process AI")
        )

        viewModel.onIntent(NewInvoiceIntent.ProcessInvoiceAi(listOf("Drywall")))

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessingAi)
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage!!.contains("Failed to process AI"))
        }
    }

    @Test
    fun `acceptAiItems moves pending to line items`() = runTest {
        val aiResult = AiInvoiceResult(
            clientName = "", clientAddress = "",
            items = listOf(LineItem("AI Item", 100.0))
        )
        coEvery { processInvoiceAiUseCase(any(), any()) } returns InvoiceTextOutcome.Success(aiResult)

        viewModel.onIntent(NewInvoiceIntent.ProcessInvoiceAi(listOf("Drywall")))
        viewModel.onIntent(NewInvoiceIntent.AcceptAiItems)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.lineItems.size)
            assertEquals("AI Item", state.lineItems[0].description)
            assertTrue(state.pendingAi.isEmpty())
            assertFalse(state.showAiConf)
        }
    }

    @Test
    fun `clearError resets error message`() = runTest {
        coEvery { processInvoiceAiUseCase(any(), any()) } returns InvoiceTextOutcome.Failure(
            FailureMessage("err")
        )
        viewModel.onIntent(NewInvoiceIntent.ProcessInvoiceAi(emptyList()))

        viewModel.onIntent(NewInvoiceIntent.ClearError)

        viewModel.uiState.test {
            assertNull(awaitItem().errorMessage)
        }
    }
}
