package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.*
import com.fordham.toolbelt.domain.model.SaveBusinessLogoOutcome
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Manages new-invoice draft state (UDF). See [NewInvoiceContract] for ui state and intents. */
class NewInvoiceViewModel(
    private val receiptRepository: ReceiptRepository,
    private val processInvoiceAiUseCase: ProcessInvoiceAiUseCase,
    private val billLaborUseCase: BillLaborUseCase,
    private val generateAndSaveInvoiceUseCase: GenerateAndSaveInvoiceUseCase,
    private val draftRepository: DraftRepository,
    private val settingsRepository: SettingsRepository,
    private val saveBusinessLogoUseCase: SaveBusinessLogoUseCase
) : ViewModel() {

    private val _transientState = MutableStateFlow(NewInvoiceTransientState())
    private val draftEditor = NewInvoiceDraftEditor(draftRepository)

    val uiState: StateFlow<NewInvoiceUiState> = combine(
        draftEditor.draft,
        _transientState,
        settingsRepository.businessSettingsFlow
    ) { draft, transient, settings ->
        val effectiveLogo = draft.logoUri?.takeIf { it.isNotBlank() } ?: settings.logoUri
        NewInvoiceUiState(
            clientName = transient.clientName ?: draft.clientName,
            clientAddress = transient.clientAddress ?: draft.clientAddress,
            taxText = transient.taxText ?: if (draft.taxRate == 0.0) "" else draft.taxRate.toString(),
            depositCollected = transient.depositCollected ?: if (draft.deposit == 0.0) "" else draft.deposit.toString(),
            hourlyRate = transient.hourlyRate ?: if (draft.hourlyRate == 0.0) "" else draft.hourlyRate.toString(),
            logoUri = effectiveLogo,
            businessLogoSaved = !settings.logoUri.isNullOrBlank(),
            lineItems = draft.lineItems,
            selectedCategory = draft.selectedCategory,
            itemDesc = transient.itemDesc ?: draft.itemDesc,
            itemAmt = transient.itemAmt ?: draft.itemAmt,
            isProcessingAi = transient.isProcessingAi,
            pendingAi = transient.pendingAi,
            showAiConf = transient.showAiConf,
            showClientDropdown = transient.showClientDropdown,
            showCategoryDropdown = transient.showCategoryDropdown,
            isListening = transient.isListening,
            timerRunning = draft.timerRunning,
            elapsedSeconds = draft.elapsedSeconds,
            startTime = draft.startTime,
            saveToClientDirectory = draft.saveToClientDirectory,
            capturedPhotos = draft.capturedPhotos,
            availableReceipts = transient.availableReceipts,
            showReceiptPicker = transient.showReceiptPicker,
            canAddManual = (transient.itemDesc ?: draft.itemDesc).isNotBlank() && ((transient.itemAmt ?: draft.itemAmt).toDoubleOrNull() ?: 0.0) > 0.0,
            canSave = (transient.clientName ?: draft.clientName).isNotBlank() && 
                      (transient.clientAddress ?: draft.clientAddress).isNotBlank() && 
                      draft.lineItems.isNotEmpty(),
            errorMessage = transient.errorMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NewInvoiceUiState())

    init {
        viewModelScope.launch {
            receiptRepository.getUnassignedReceipts().collect { result ->
                if (result is ReceiptListOutcome.Success) {
                    _transientState.update { it.copy(availableReceipts = result.receipts) }
                }
            }
        }

        // Collect database draft emissions and synchronize transient state when they differ.
        // This prevents cursor jumps during manual typing while ensuring background updates
        // (like those from the Foreman voice agent) are immediately reflected in the UI.
        viewModelScope.launch {
            val settings = settingsRepository.getBusinessSettings()
            val initialDraft = draftEditor.currentDraft()
            if (initialDraft.logoUri.isNullOrBlank() && !settings.logoUri.isNullOrBlank()) {
                draftEditor.updateDraft { it.copy(logoUri = settings.logoUri) }
            }
            
            if (initialDraft.timerRunning) {
                viewModelScope.launch {
                    draftEditor.resumeTimerLoop()
                }
            }
            
            draftEditor.draft.collect { draft ->
                _transientState.update { state ->
                    val nextTaxText = if ((state.taxText?.toDoubleOrNull() ?: 0.0) != draft.taxRate) {
                        if (draft.taxRate == 0.0) "" else draft.taxRate.toString()
                    } else {
                        state.taxText
                    }
                    val nextDepositCollected = if ((state.depositCollected?.toDoubleOrNull() ?: 0.0) != draft.deposit) {
                        if (draft.deposit == 0.0) "" else draft.deposit.toString()
                    } else {
                        state.depositCollected
                    }
                    val nextHourlyRate = if ((state.hourlyRate?.toDoubleOrNull() ?: 0.0) != draft.hourlyRate) {
                        if (draft.hourlyRate == 0.0) "" else draft.hourlyRate.toString()
                    } else {
                        state.hourlyRate
                    }

                    state.copy(
                        clientName = if (state.clientName != draft.clientName) draft.clientName else state.clientName,
                        clientAddress = if (state.clientAddress != draft.clientAddress) draft.clientAddress else state.clientAddress,
                        taxText = nextTaxText,
                        depositCollected = nextDepositCollected,
                        hourlyRate = nextHourlyRate,
                        itemDesc = if (state.itemDesc != draft.itemDesc) draft.itemDesc else state.itemDesc,
                        itemAmt = if (state.itemAmt != draft.itemAmt) draft.itemAmt else state.itemAmt
                    )
                }
            }
        }
    }

    val categories = listOf("Drywall", "Flooring", "Roofing", "Plumbing", "Electrical", "Painting", "Carpentry", "General Repair")

    fun onIntent(intent: NewInvoiceIntent) {
        when (intent) {
            is NewInvoiceIntent.OnClientNameChange -> {
                _transientState.update { it.copy(clientName = intent.name) }
                updateDraft { it.copy(clientName = intent.name) }
            }
            is NewInvoiceIntent.OnClientAddressChange -> {
                _transientState.update { it.copy(clientAddress = intent.address) }
                updateDraft { it.copy(clientAddress = intent.address) }
            }
            is NewInvoiceIntent.OnTaxTextChange -> {
                _transientState.update { it.copy(taxText = intent.tax) }
                updateDraft { it.copy(taxRate = intent.tax.toDoubleOrNull() ?: 0.0) }
            }
            is NewInvoiceIntent.OnDepositCollectedChange -> {
                _transientState.update { it.copy(depositCollected = intent.amt) }
                updateDraft { it.copy(deposit = intent.amt.toDoubleOrNull() ?: 0.0) }
            }
            is NewInvoiceIntent.OnHourlyRateChange -> {
                _transientState.update { it.copy(hourlyRate = intent.rate) }
                updateDraft { it.copy(hourlyRate = intent.rate.toDoubleOrNull() ?: 0.0) }
            }
            is NewInvoiceIntent.OnLogoUriChange -> executeSaveBusinessLogo(intent.uri)
            is NewInvoiceIntent.OnCategoryChange -> updateDraft { it.copy(selectedCategory = intent.cat) }
            is NewInvoiceIntent.OnSaveToClientDirectoryChange -> updateDraft { it.copy(saveToClientDirectory = intent.save) }
            is NewInvoiceIntent.OnItemDescChange -> {
                _transientState.update { it.copy(itemDesc = intent.desc) }
                updateDraft { it.copy(itemDesc = intent.desc) }
            }
            is NewInvoiceIntent.OnItemAmtChange -> {
                _transientState.update { it.copy(itemAmt = intent.amt) }
                updateDraft { it.copy(itemAmt = intent.amt) }
            }
            is NewInvoiceIntent.OnPhotoCaptured -> executeAddPhoto(intent.uri, intent.phase)
            is NewInvoiceIntent.RemovePhoto -> executeRemovePhoto(intent.uri)
            is NewInvoiceIntent.SetClientDropdownVisible -> _transientState.update { it.copy(showClientDropdown = intent.visible) }
            is NewInvoiceIntent.SetCategoryDropdownVisible -> _transientState.update { it.copy(showCategoryDropdown = intent.visible) }
            is NewInvoiceIntent.OnListeningStateChange -> _transientState.update { it.copy(isListening = intent.visible) }
            is NewInvoiceIntent.OnShowAiConfChange -> _transientState.update { it.copy(showAiConf = intent.visible) }
            is NewInvoiceIntent.SetReceiptPickerVisible -> _transientState.update { it.copy(showReceiptPicker = intent.visible) }
            is NewInvoiceIntent.ClearError -> _transientState.update { it.copy(errorMessage = null) }
            is NewInvoiceIntent.ToggleTimer -> executeToggleTimer()
            is NewInvoiceIntent.AddManualLineItem -> executeAddManualItem()
            is NewInvoiceIntent.BillLabor -> executeBillLabor()
            is NewInvoiceIntent.RemoveLineItem -> executeRemoveLineItem(intent.item)
            is NewInvoiceIntent.AcceptAiItems -> executeAcceptAiItems()
            is NewInvoiceIntent.ProcessInvoiceAi -> executeProcessAi(intent.categories)
            is NewInvoiceIntent.LinkReceipt -> executeLinkReceipt(intent.receipt, intent.markupPercent)
            is NewInvoiceIntent.SaveInvoice -> executeSaveInvoice(intent.isEstimate, intent.settings, intent.onGenerated)
        }
    }

    private fun updateDraft(update: (DraftInvoice) -> DraftInvoice) {
        viewModelScope.launch {
            draftEditor.updateDraft(update)
        }
    }

    private fun executeSaveBusinessLogo(pickedUri: String?) {
        viewModelScope.launch {
            when (val outcome = saveBusinessLogoUseCase(pickedUri)) {
                is SaveBusinessLogoOutcome.Saved -> {
                    updateDraft { it.copy(logoUri = outcome.stablePath) }
                    _transientState.update { it.copy(errorMessage = null) }
                }
                is SaveBusinessLogoOutcome.Cleared -> updateDraft { it.copy(logoUri = null) }
                is SaveBusinessLogoOutcome.Failure ->
                    _transientState.update { it.copy(errorMessage = outcome.error.value) }
            }
        }
    }

    private fun executeAddPhoto(uri: String, phase: JobPhotoPhase) = updateDraft {
        it.copy(capturedPhotos = it.capturedPhotos + CapturedJobPhoto(uri = uri, phase = phase))
    }

    private fun executeRemovePhoto(uri: String) = updateDraft {
        it.copy(capturedPhotos = it.capturedPhotos.filterNot { photo -> photo.uri == uri })
    }

    private fun executeToggleTimer() {
        viewModelScope.launch {
            draftEditor.toggleTimer()
        }
    }

    private fun executeAddManualItem() {
        viewModelScope.launch {
            if (draftEditor.addManualItem()) {
                _transientState.update { it.copy(itemDesc = "", itemAmt = "") }
            }
        }
    }

    private fun executeBillLabor() = viewModelScope.launch { billLaborUseCase() }

    private fun executeRemoveLineItem(item: LineItem) = updateDraft { 
        it.copy(lineItems = it.lineItems - item)
    }

    private fun executeAcceptAiItems() {
        viewModelScope.launch {
            draftEditor.acceptAiItems(_transientState.value.pendingAi)
            _transientState.update { it.copy(showAiConf = false, pendingAi = emptyList(), itemDesc = "") }
        }
    }

    private fun executeProcessAi(categories: List<String>) {
        viewModelScope.launch {
            val draft = draftEditor.currentDraft()
            _transientState.update { it.copy(isProcessingAi = true, errorMessage = null) }
            val result = processInvoiceAiUseCase(draft.itemDesc, categories)
            if (result is InvoiceTextOutcome.Success) {
                val ai = result.result
                draftEditor.updateDraft { current -> current.copy(
                    clientName = if (ai.clientName.isNotBlank()) ai.clientName else draft.clientName,
                    clientAddress = if (ai.clientAddress.isNotBlank()) ai.clientAddress else draft.clientAddress
                ) }
                _transientState.update { state ->
                    state.copy(
                        pendingAi = ai.items,
                        showAiConf = ai.items.isNotEmpty(),
                        clientName = if (ai.clientName.isNotBlank()) ai.clientName else state.clientName,
                        clientAddress = if (ai.clientAddress.isNotBlank()) ai.clientAddress else state.clientAddress
                    )
                }
            } else if (result is InvoiceTextOutcome.Failure) {
                _transientState.update { it.copy(errorMessage = "AI Error: ${result.error.value}") }
            }
            _transientState.update { it.copy(isProcessingAi = false) }
        }
    }

    private fun executeLinkReceipt(receipt: ReceiptItem, markup: Double) {
        viewModelScope.launch {
            draftEditor.linkReceipt(receipt, markup)
            _transientState.update { it.copy(showReceiptPicker = false) }
        }
    }

    private fun executeSaveInvoice(isEst: Boolean, set: BusinessSettings, onGen: (String) -> Unit) {
        viewModelScope.launch {
            println("SAVE_INVOICE: Starting executeSaveInvoice...")
            val draft = draftEditor.currentDraft()
            
            val finalClientName = (_transientState.value.clientName ?: draft.clientName).trim()
            val finalClientAddress = (_transientState.value.clientAddress ?: draft.clientAddress).trim()
            
            if (finalClientName.isBlank()) {
                _transientState.update { it.copy(errorMessage = "Client Name must be filled out to save.") }
                return@launch
            }
            if (finalClientAddress.isBlank()) {
                _transientState.update { it.copy(errorMessage = "Client Address must be filled out to save.") }
                return@launch
            }
            if (draft.lineItems.isEmpty()) {
                _transientState.update { it.copy(errorMessage = "Job Description / Line Items must be filled out to save. Please enter Category, Description, and Price, then tap 'ADD ITEM'.") }
                return@launch
            }
            
            val settings = settingsRepository.getBusinessSettings()
            val logoForPdf = draft.logoUri?.takeIf { it.isNotBlank() } ?: settings.logoUri
            println("SAVE_INVOICE: Loaded draft with client: $finalClientName, items: ${draft.lineItems}")
            val res = generateAndSaveInvoiceUseCase(
                GenerateInvoiceRequest(
                    clientName = ClientName(finalClientName),
                    clientAddress = ClientAddress(finalClientAddress),
                    saveToClientDirectory = draft.saveToClientDirectory,
                    taxRate = TaxRatePercent(draft.taxRate),
                    deposit = MoneyAmount(maxOf(0.0, draft.deposit)),
                    lineItems = draft.lineItems,
                    logoUriString = logoForPdf?.takeIf { it.isNotBlank() }?.let { MediaUri(it) },
                    businessSettings = set,
                    isEstimate = isEst,
                    elapsedSeconds = DurationSeconds(draft.elapsedSeconds),
                    capturedPhotos = draft.capturedPhotos,
                    linkedReceiptIds = draft.linkedReceiptIds.map { ReceiptId(it) },
                    availableReceipts = _transientState.value.availableReceipts,
                    onGenerated = { onGen(it.value) }
                )
            )
            when (res) {
                is GenerateInvoiceOutcome.Success -> {
                    println("SAVE_INVOICE: Saved successfully! Clearing draft...")
                    draftEditor.clearDraft()
                    _transientState.update {
                        it.copy(
                            clientName = "",
                            clientAddress = "",
                            taxText = "7.0",
                            depositCollected = "",
                            hourlyRate = "50.0",
                            itemDesc = "",
                            itemAmt = ""
                        )
                    }
                }
                is GenerateInvoiceOutcome.Error -> {
                    println("SAVE_INVOICE_ERROR: ${res.message}")
                    _transientState.update { it.copy(errorMessage = res.message) }
                }
            }
        }
    }
}
