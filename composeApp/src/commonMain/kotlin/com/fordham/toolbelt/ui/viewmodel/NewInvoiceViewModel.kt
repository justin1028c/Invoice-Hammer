package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.*
import com.fordham.toolbelt.domain.model.SaveBusinessLogoOutcome
import com.fordham.toolbelt.ui.InvoiceCategories
import com.fordham.toolbelt.util.AppLogger
import com.fordham.toolbelt.util.UiMessageKeys
import com.fordham.toolbelt.util.VoiceInvoiceLogRedactor
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
    private val saveBusinessLogoUseCase: SaveBusinessLogoUseCase,
    private val buildVoiceInvoiceApplicationPlan: BuildVoiceInvoiceApplicationPlanUseCase
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
            errorMessage = transient.errorMessage,
            laborHours = transient.laborHours,
            laborRate = transient.laborRate,
            depositAmount = transient.depositAmount ?: 0.0,
            taxRatePercent = transient.taxRatePercent ?: 7.0,
            discountPercent = transient.discountPercent ?: 0.0,
            notes = transient.notes.orEmpty(),
            confidenceScore = transient.confidenceScore ?: 1.0,
            userSummary = transient.userSummary.orEmpty(),
            validationIssues = transient.validationIssues ?: emptyList()
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
            
            var lastDraft: DraftInvoice? = null
            draftEditor.draft.collect { draft ->
                _transientState.update { state ->
                    val prev = lastDraft
                    
                    val clientNameChangedInDb = prev != null && prev.clientName != draft.clientName
                    val clientAddressChangedInDb = prev != null && prev.clientAddress != draft.clientAddress
                    val taxRateChangedInDb = prev != null && prev.taxRate != draft.taxRate
                    val depositChangedInDb = prev != null && prev.deposit != draft.deposit
                    val hourlyRateChangedInDb = prev != null && prev.hourlyRate != draft.hourlyRate
                    val itemDescChangedInDb = prev != null && prev.itemDesc != draft.itemDesc
                    val itemAmtChangedInDb = prev != null && prev.itemAmt != draft.itemAmt

                    val nextClientName = if (clientNameChangedInDb) draft.clientName else (state.clientName ?: draft.clientName)
                    val nextClientAddress = if (clientAddressChangedInDb) draft.clientAddress else (state.clientAddress ?: draft.clientAddress)

                    val nextTaxText = if (taxRateChangedInDb) {
                        if (draft.taxRate == 0.0) "" else draft.taxRate.toString()
                    } else {
                        state.taxText ?: if (draft.taxRate == 0.0) "" else draft.taxRate.toString()
                    }
                    val nextDepositCollected = if (depositChangedInDb) {
                        if (draft.deposit == 0.0) "" else draft.deposit.toString()
                    } else {
                        state.depositCollected ?: if (draft.deposit == 0.0) "" else draft.deposit.toString()
                    }
                    val nextHourlyRate = if (hourlyRateChangedInDb) {
                        if (draft.hourlyRate == 0.0) "" else draft.hourlyRate.toString()
                    } else {
                        state.hourlyRate ?: if (draft.hourlyRate == 0.0) "" else draft.hourlyRate.toString()
                    }

                    val nextItemDesc = if (itemDescChangedInDb) draft.itemDesc else (state.itemDesc ?: draft.itemDesc)
                    val nextItemAmt = if (itemAmtChangedInDb) draft.itemAmt else (state.itemAmt ?: draft.itemAmt)

                    state.copy(
                        clientName = nextClientName,
                        clientAddress = nextClientAddress,
                        taxText = nextTaxText,
                        depositCollected = nextDepositCollected,
                        hourlyRate = nextHourlyRate,
                        itemDesc = nextItemDesc,
                        itemAmt = nextItemAmt
                    )
                }
                lastDraft = draft
            }
        }
    }

    val categories = InvoiceCategories.englishKeys

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
            is NewInvoiceIntent.UpdateLineItem -> executeUpdateLineItem(intent.original, intent.updated)
            is NewInvoiceIntent.AcceptAiItems -> executeAcceptAiItems()
            is NewInvoiceIntent.ProcessInvoiceAi -> executeProcessAi(intent.categories)
            is NewInvoiceIntent.LinkReceipt -> executeLinkReceipt(intent.receipt, intent.markupPercent)
            is NewInvoiceIntent.SaveInvoice -> executeSaveInvoice(intent.isEstimate, intent.settings, intent.onGenerated)
        }
    }

    fun loadInvoiceForEditing(invoice: Invoice) {
        viewModelScope.launch {
            val settings = settingsRepository.getBusinessSettings()
            val draft = DraftInvoice(
                clientName = invoice.clientName.value,
                clientAddress = invoice.clientAddress.value,
                taxRate = settings.taxRate,
                deposit = invoice.depositAmount.value,
                hourlyRate = 50.0,
                logoUri = settings.logoUri,
                selectedCategory = "General Repair",
                saveToClientDirectory = true,
                lineItems = listOf(
                    LineItem(
                        description = invoice.itemsSummary,
                        amount = invoice.totalAmount,
                        category = "General Repair",
                        quantity = 1.0,
                        unitPrice = invoice.totalAmount
                    )
                )
            )
            draftRepository.saveDraft(draft)
            _transientState.update {
                it.copy(
                    clientName = invoice.clientName.value,
                    clientAddress = invoice.clientAddress.value,
                    taxText = settings.taxRate.toString(),
                    depositCollected = if (invoice.depositAmount.value == 0.0) "" else invoice.depositAmount.value.toString(),
                    hourlyRate = "50.0",
                    itemDesc = "",
                    itemAmt = "",
                    pendingAi = emptyList(),
                    showAiConf = false,
                    errorMessage = null
                )
            }
        }
    }

    fun startInvoiceForClient(client: Client) {
        viewModelScope.launch {
            val settings = settingsRepository.getBusinessSettings()
            draftRepository.saveDraft(
                DraftInvoice(
                    clientName = client.name.value,
                    clientAddress = client.address.value,
                    taxRate = settings.taxRate,
                    hourlyRate = 50.0,
                    logoUri = settings.logoUri,
                    saveToClientDirectory = true
                )
            )
            _transientState.update {
                it.copy(
                    clientName = client.name.value,
                    clientAddress = client.address.value,
                    taxText = settings.taxRate.toString(),
                    depositCollected = "",
                    hourlyRate = "50.0",
                    itemDesc = "",
                    itemAmt = "",
                    pendingAi = emptyList(),
                    showAiConf = false,
                    errorMessage = null
                )
            }
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

    private fun executeUpdateLineItem(original: LineItem, updated: LineItem) = updateDraft { draft ->
        draft.copy(
            lineItems = draft.lineItems.map { item ->
                if (item == original) updated else item
            }
        )
    }

    private fun executeAcceptAiItems() {
        viewModelScope.launch {
            val pending = _transientState.value.pendingAi
            AppLogger.d(
                "VoiceInvoicePipeline",
                "UI_ACCEPT pendingItems=${pending.size} lines=${pending.joinToString(" | ") { "${it.description.value}:${it.amount.value}:qty=${it.quantity}:unit=${it.unitPrice?.value}:cat=${it.category}" }}"
            )
            draftEditor.acceptAiItems(pending)
            _transientState.update { it.copy(showAiConf = false, pendingAi = emptyList(), itemDesc = "") }
        }
    }

    private fun executeProcessAi(categories: List<String>) {
        viewModelScope.launch {
            val draft = draftEditor.currentDraft()
            _transientState.update { it.copy(isProcessingAi = true, errorMessage = null) }
            AppLogger.d(
                "VoiceInvoicePipeline",
                "UI_START text=${VoiceInvoiceLogRedactor.transcriptMeta(draft.itemDesc)} draftClient='${draft.clientName}' " +
                    "draftAddress='${draft.clientAddress}' categories=${categories.size}"
            )
            val result = processInvoiceAiUseCase(draft.itemDesc, categories)
            if (result is InvoiceTextOutcome.Success) {
                val ai = result.result
                val plan = buildVoiceInvoiceApplicationPlan(draft, ai)
                AppLogger.d(
                    "VoiceInvoicePipeline",
                    "UI_SUCCESS aiClient='${ai.clientName}' aiAddress='${ai.clientAddress}' " +
                        "planClient='${plan.clientName.orEmpty()}' planAddress='${plan.clientAddress.orEmpty()}' " +
                        "items=${plan.pendingLineItems.size} confidence=${plan.confidenceScore} issues=${plan.validationIssues} " +
                        "lines=${plan.pendingLineItems.joinToString(" | ") { "${it.description.value}:${it.amount.value}:qty=${it.quantity}:unit=${it.unitPrice?.value}:cat=${it.category}" }}"
                )
                draftEditor.updateDraft { current -> current.copy(
                    clientName = plan.clientName ?: current.clientName,
                    clientAddress = plan.clientAddress ?: current.clientAddress,
                    taxRate = plan.taxRatePercent ?: current.taxRate,
                    deposit = plan.depositAmount ?: current.deposit,
                    hourlyRate = plan.hourlyRate ?: current.hourlyRate
                ) }
                _transientState.update { state ->
                    state.copy(
                        pendingAi = plan.pendingLineItems,
                        showAiConf = plan.pendingLineItems.isNotEmpty(),
                        clientName = plan.clientName ?: state.clientName,
                        clientAddress = plan.clientAddress ?: state.clientAddress,
                        taxText = plan.taxRatePercent?.toString() ?: state.taxText,
                        depositCollected = plan.depositAmount?.toString() ?: state.depositCollected,
                        hourlyRate = (plan.hourlyRate ?: state.hourlyRate?.toDoubleOrNull())?.toString() ?: state.hourlyRate,
                        laborHours = plan.laborHours,
                        laborRate = plan.laborRate,
                        depositAmount = plan.depositAmount ?: 0.0,
                        taxRatePercent = plan.taxRatePercent ?: state.taxRatePercent,
                        discountPercent = plan.discountPercent,
                        notes = plan.notes,
                        confidenceScore = plan.confidenceScore,
                        userSummary = plan.userSummary,
                        validationIssues = plan.validationIssues
                    )
                }
            } else if (result is InvoiceTextOutcome.Failure) {
                AppLogger.d(
                    "VoiceInvoicePipeline",
                    "UI_FAILURE error='${result.error.value}' text=${VoiceInvoiceLogRedactor.transcriptMeta(draft.itemDesc)}"
                )
                _transientState.update { it.copy(errorMessage = UiMessageKeys.aiError(result.error.value)) }
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
                _transientState.update { it.copy(errorMessage = UiMessageKeys.CLIENT_NAME_REQUIRED) }
                return@launch
            }
            if (finalClientAddress.isBlank()) {
                _transientState.update { it.copy(errorMessage = UiMessageKeys.CLIENT_ADDRESS_REQUIRED) }
                return@launch
            }
            if (draft.lineItems.isEmpty()) {
                _transientState.update { it.copy(errorMessage = UiMessageKeys.LINE_ITEMS_REQUIRED) }
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
                            itemAmt = "",
                            laborHours = null,
                            laborRate = null,
                            depositAmount = 0.0,
                            taxRatePercent = 7.0,
                            discountPercent = 0.0,
                            notes = "",
                            confidenceScore = 1.0,
                            userSummary = "",
                            validationIssues = emptyList()
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
