package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Responsibility: Manage state for creating a new invoice, utilizing a persistent draft.
 * Follows UDF pattern per AI_ARCHITECTURE_RULES.md.
 */
data class NewInvoiceUiState(
    val clientName: String = "",
    val clientAddress: String = "",
    val taxText: String = "7.0",
    val depositCollected: String = "",
    val hourlyRate: String = "50.0",
    val logoUri: String? = null,
    val lineItems: List<LineItem> = emptyList(),
    val selectedCategory: String = "Drywall",
    val itemDesc: String = "",
    val itemAmt: String = "",
    val isProcessingAi: Boolean = false,
    val pendingAi: List<LineItem> = emptyList(),
    val showAiConf: Boolean = false,
    val showClientDropdown: Boolean = false,
    val showCategoryDropdown: Boolean = false,
    val isListening: Boolean = false,
    val timerRunning: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val startTime: Long = 0L,
    val saveToClientDirectory: Boolean = false,
    val canAddManual: Boolean = false,
    val canSave: Boolean = false,
    val errorMessage: String? = null,
    val capturedPhotos: List<String> = emptyList(),
    val availableReceipts: List<ReceiptItem> = emptyList(),
    val showReceiptPicker: Boolean = false
) {
    val formattedTime: String get() {
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

sealed interface NewInvoiceIntent {
    data class OnClientNameChange(val name: String) : NewInvoiceIntent
    data class OnClientAddressChange(val address: String) : NewInvoiceIntent
    data class OnTaxTextChange(val tax: String) : NewInvoiceIntent
    data class OnDepositCollectedChange(val amt: String) : NewInvoiceIntent
    data class OnHourlyRateChange(val rate: String) : NewInvoiceIntent
    data class OnLogoUriChange(val uri: String?) : NewInvoiceIntent
    data class OnCategoryChange(val cat: String) : NewInvoiceIntent
    data class OnSaveToClientDirectoryChange(val save: Boolean) : NewInvoiceIntent
    data class OnItemDescChange(val desc: String) : NewInvoiceIntent
    data class OnItemAmtChange(val amt: String) : NewInvoiceIntent
    data class OnPhotoCaptured(val uri: String) : NewInvoiceIntent
    data class RemovePhoto(val uri: String) : NewInvoiceIntent
    data class SetClientDropdownVisible(val visible: Boolean) : NewInvoiceIntent
    data class SetCategoryDropdownVisible(val visible: Boolean) : NewInvoiceIntent
    data class OnListeningStateChange(val visible: Boolean) : NewInvoiceIntent
    data class OnShowAiConfChange(val visible: Boolean) : NewInvoiceIntent
    data class SetReceiptPickerVisible(val visible: Boolean) : NewInvoiceIntent
    object ClearError : NewInvoiceIntent
    object ToggleTimer : NewInvoiceIntent
    object AddManualLineItem : NewInvoiceIntent
    object BillLabor : NewInvoiceIntent
    data class RemoveLineItem(val item: LineItem) : NewInvoiceIntent
    object AcceptAiItems : NewInvoiceIntent
    data class ProcessInvoiceAi(val categories: List<String>) : NewInvoiceIntent
    data class LinkReceipt(val receipt: ReceiptItem, val markupPercent: Double) : NewInvoiceIntent
    data class SaveInvoice(val isEstimate: Boolean, val settings: BusinessSettings, val onGenerated: (String) -> Unit) : NewInvoiceIntent
}

class NewInvoiceViewModel(
    private val receiptRepository: ReceiptRepository,
    private val processInvoiceAiUseCase: ProcessInvoiceAiUseCase,
    private val billLaborUseCase: BillLaborUseCase,
    private val generateAndSaveInvoiceUseCase: GenerateAndSaveInvoiceUseCase,
    private val draftRepository: DraftRepository
) : ViewModel() {

    private val _transientState = MutableStateFlow(TransientUiState())
    private val draftEditor = NewInvoiceDraftEditor(draftRepository)
    
    data class TransientUiState(
        val isProcessingAi: Boolean = false,
        val isListening: Boolean = false,
        val errorMessage: String? = null,
        val showAiConf: Boolean = false,
        val pendingAi: List<LineItem> = emptyList(),
        val showClientDropdown: Boolean = false,
        val showCategoryDropdown: Boolean = false,
        val showReceiptPicker: Boolean = false,
        val availableReceipts: List<ReceiptItem> = emptyList(),
        val clientName: String? = null,
        val clientAddress: String? = null,
        val taxText: String? = null,
        val depositCollected: String? = null,
        val hourlyRate: String? = null,
        val itemDesc: String? = null,
        val itemAmt: String? = null
    )

    val uiState: StateFlow<NewInvoiceUiState> = combine(
        draftEditor.draft,
        _transientState
    ) { draft, transient ->
        NewInvoiceUiState(
            clientName = transient.clientName ?: draft.clientName,
            clientAddress = transient.clientAddress ?: draft.clientAddress,
            taxText = transient.taxText ?: draft.taxRate.toString(),
            depositCollected = transient.depositCollected ?: draft.deposit.toString(),
            hourlyRate = transient.hourlyRate ?: draft.hourlyRate.toString(),
            logoUri = draft.logoUri,
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
            canSave = (transient.clientName ?: draft.clientName).isNotBlank() && draft.lineItems.isNotEmpty(),
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

        // Bootstrap transient state with initial saved draft values to prevent cursor jumps
        viewModelScope.launch {
            val initialDraft = draftEditor.currentDraft()
            _transientState.update { 
                it.copy(
                    clientName = initialDraft.clientName,
                    clientAddress = initialDraft.clientAddress,
                    taxText = initialDraft.taxRate.toString(),
                    depositCollected = initialDraft.deposit.toString(),
                    hourlyRate = initialDraft.hourlyRate.toString(),
                    itemDesc = initialDraft.itemDesc,
                    itemAmt = initialDraft.itemAmt
                )
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
            is NewInvoiceIntent.OnLogoUriChange -> updateDraft { it.copy(logoUri = intent.uri) }
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
            is NewInvoiceIntent.OnPhotoCaptured -> executeAddPhoto(intent.uri)
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

    private fun executeAddPhoto(uri: String) = updateDraft { 
        it.copy(capturedPhotos = it.capturedPhotos + uri)
    }

    private fun executeRemovePhoto(uri: String) = updateDraft { 
        it.copy(capturedPhotos = it.capturedPhotos - uri)
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
            println("SAVE_INVOICE: Loaded draft with client: ${draft.clientName}, items: ${draft.lineItems}")
            val res = generateAndSaveInvoiceUseCase(
                GenerateInvoiceRequest(
                    clientName = draft.clientName,
                    clientAddress = draft.clientAddress,
                    saveToClientDirectory = draft.saveToClientDirectory,
                    taxRate = draft.taxRate,
                    deposit = draft.deposit,
                    lineItems = draft.lineItems,
                    logoUriString = draft.logoUri,
                    businessSettings = set,
                    isEstimate = isEst,
                    elapsedSeconds = draft.elapsedSeconds,
                    capturedPhotos = draft.capturedPhotos,
                    linkedReceiptIds = draft.linkedReceiptIds,
                    availableReceipts = _transientState.value.availableReceipts,
                    onGenerated = onGen
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
