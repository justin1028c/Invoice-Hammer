package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.ProcessReceiptOutcome
import com.fordham.toolbelt.domain.model.StorageBytesOutcome
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.StorageRepository
import com.fordham.toolbelt.domain.usecase.ProcessReceiptRequest
import com.fordham.toolbelt.domain.usecase.ProcessReceiptUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReceiptsUiState(
    val capturedImageBytes: ByteArray? = null,
    val isProcessing: Boolean = false,
    val showClearConfirmDialog: Boolean = false,
    val showClientDropdown: Boolean = false,
    val showMarkupDialog: Boolean = false,
    val markupPercentage: String = "0",
    val errorMessage: String? = null
)

class ReceiptsViewModel(
    private val receiptRepository: ReceiptRepository,
    private val storageRepository: StorageRepository,
    private val processReceiptUseCase: ProcessReceiptUseCase,
    private val settingsRepository: com.fordham.toolbelt.domain.repository.SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptsUiState())
    val uiState: StateFlow<ReceiptsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.businessSettingsFlow.collect { settings ->
                _uiState.update { it.copy(markupPercentage = settings.markupPercentage.toInt().toString()) }
            }
        }
    }

    private val _filterClientName = MutableStateFlow<String?>(null)
    val filterClientName: StateFlow<String?> = _filterClientName.asStateFlow()

    val filteredReceipts: Flow<List<ReceiptItem>> = combine(
        receiptRepository.allItems,
        _filterClientName
    ) { itemsResult, filter ->
        val items = if (itemsResult is ReceiptListOutcome.Success) {
            itemsResult.receipts
        } else emptyList()

        if (filter == null) items.filter { it.clientName == "General" || it.clientName.isEmpty() }
        else items.filter { it.clientName == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val receiptsTotal: Flow<Double> = filteredReceipts.map { items ->
        items.sumOf { it.totalPrice }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalWithMarkup: Flow<Double> = combine(
        receiptsTotal,
        _uiState.map { it.markupPercentage }
    ) { total, markupStr ->
        val markup = markupStr.toDoubleOrNull() ?: 0.0
        total * (1.0 + (markup / 100.0))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setFilterClient(name: String?) {
        _filterClientName.value = name
    }

    fun setClearConfirmVisible(visible: Boolean) { _uiState.update { it.copy(showClearConfirmDialog = visible) } }
    
    fun clearReceiptItems() {
        viewModelScope.launch { receiptRepository.deleteAllItems() }
    }

    fun onReceiptImageCaptured(bytes: ByteArray?) {
        _uiState.update { it.copy(capturedImageBytes = bytes) }
    }

    fun clearCapturedReceiptImage() {
        _uiState.update { it.copy(capturedImageBytes = null, errorMessage = null) }
    }

    fun onReceiptUriSelected(uri: String) {
        viewModelScope.launch {
            val result = storageRepository.getBytesFromUri(uri)
            if (result is StorageBytesOutcome.Success) {
                onReceiptImageCaptured(result.bytes)
            }
        }
    }

    fun setClientDropdownVisible(visible: Boolean) { _uiState.update { it.copy(showClientDropdown = visible) } }
    fun setMarkupDialogVisible(visible: Boolean) { _uiState.update { it.copy(showMarkupDialog = visible) } }
    fun onMarkupPercentageChange(pct: String) { 
        _uiState.update { it.copy(markupPercentage = pct) } 
        viewModelScope.launch {
            val markup = pct.toDoubleOrNull() ?: 0.0
            val current = settingsRepository.businessSettingsFlow.first()
            settingsRepository.saveBusinessSettings(current.copy(markupPercentage = markup))
        }
    }
    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }

    fun deleteReceiptItem(item: ReceiptItem) {
        viewModelScope.launch { receiptRepository.deleteItem(item) }
    }
    
    fun updateReceiptItem(item: ReceiptItem) {
        viewModelScope.launch { receiptRepository.updateItem(item) }
    }

    fun toggleReceiptBilled(item: ReceiptItem) {
        updateReceiptItem(item.copy(isBilled = !item.isBilled))
    }

    fun processCapturedReceipt(selectedClient: Client?, onComplete: () -> Unit = {}) {
        val imageBytes = _uiState.value.capturedImageBytes ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            
            val result = processReceiptUseCase(
                ProcessReceiptRequest(
                    imageBytes = imageBytes,
                    clientName = selectedClient?.name
                )
            )
            when (result) {
                is ProcessReceiptOutcome.Success -> {
                    _uiState.update { it.copy(capturedImageBytes = null) }
                }
                is ProcessReceiptOutcome.Failure -> {
                    _uiState.update { it.copy(errorMessage = "Failed to process receipt: ${result.error.value}") }
                }
                is ProcessReceiptOutcome.PremiumRequired -> {
                    _uiState.update { it.copy(errorMessage = "Premium subscription required to process receipt") }
                }
            }
            _uiState.update { it.copy(isProcessing = false) }
            onComplete()
        }
    }
}
