package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Responsibility: Manage UI state and handle user intents for the Clients feature.
 * Follows UDF pattern per AI_ARCHITECTURE_RULES.md.
 */
data class ClientsUiState(
    val clientToDelete: Client? = null,
    val showAddNote: Boolean = false,
    val noteText: String = "",
    val aiSummary: String? = null,
    val isSummarizing: Boolean = false,
    val availableReceipts: List<ReceiptItem> = emptyList(),
    val showReceiptPicker: Boolean = false
)

sealed interface ClientsIntent {
    data class SetClientToDelete(val client: Client?) : ClientsIntent
    data class DeleteClient(val client: Client) : ClientsIntent
    data class SetAddNoteVisible(val visible: Boolean) : ClientsIntent
    data class OnNoteTextChange(val text: String) : ClientsIntent
    data class AddNote(val clientName: String) : ClientsIntent
    data class DeleteNote(val note: JobNote) : ClientsIntent
    data class SummarizeNotes(val notes: List<JobNote>) : ClientsIntent
    object ClearAiSummary : ClientsIntent
    data class SetReceiptPickerVisible(val visible: Boolean) : ClientsIntent
    data class LinkReceipt(val receipt: ReceiptItem, val clientName: String) : ClientsIntent
    data class OnPhotoCaptured(val uriString: String, val invoiceId: String) : ClientsIntent
}

class ClientsViewModel(
    private val clientRepository: ClientRepository,
    private val receiptRepository: ReceiptRepository,
    private val addJobNoteUseCase: AddJobNoteUseCase,
    private val deleteJobNoteUseCase: DeleteJobNoteUseCase,
    private val deleteClientUseCase: DeleteClientUseCase,
    private val linkReceiptToClientUseCase: LinkReceiptToClientUseCase,
    private val saveJobPhotoUseCase: SaveJobPhotoUseCase,
    private val generateSummaryUseCase: GenerateSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientsUiState())
    val uiState: StateFlow<ClientsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            receiptRepository.getUnassignedReceipts().collect { result ->
                if (result is ReceiptListOutcome.Success) {
                    _uiState.update { it.copy(availableReceipts = result.receipts) }
                }
            }
        }
    }

    val allClients: Flow<List<Client>> = clientRepository.getAllClients()
        .map { result -> if (result is ClientListOutcome.Success) result.clients else emptyList() }

    fun onIntent(intent: ClientsIntent) {
        when (intent) {
            is ClientsIntent.SetClientToDelete -> _uiState.update { it.copy(clientToDelete = intent.client) }
            is ClientsIntent.DeleteClient -> executeDeleteClient(intent.client)
            is ClientsIntent.SetAddNoteVisible -> _uiState.update { it.copy(showAddNote = intent.visible) }
            is ClientsIntent.OnNoteTextChange -> _uiState.update { it.copy(noteText = intent.text) }
            is ClientsIntent.AddNote -> executeAddNote(intent.clientName)
            is ClientsIntent.DeleteNote -> executeDeleteNote(intent.note)
            is ClientsIntent.SummarizeNotes -> executeSummarizeNotes(intent.notes)
            is ClientsIntent.ClearAiSummary -> _uiState.update { it.copy(aiSummary = null) }
            is ClientsIntent.SetReceiptPickerVisible -> _uiState.update { it.copy(showReceiptPicker = intent.visible) }
            is ClientsIntent.LinkReceipt -> executeLinkReceipt(intent.receipt, intent.clientName)
            is ClientsIntent.OnPhotoCaptured -> executeSavePhoto(intent.uriString, intent.invoiceId)
        }
    }

    private fun executeDeleteClient(client: Client) {
        viewModelScope.launch {
            deleteClientUseCase(client)
        }
    }

    private fun executeAddNote(clientName: String) {
        val text = _uiState.value.noteText
        if (text.isBlank()) return
        viewModelScope.launch {
            val result = addJobNoteUseCase(clientName, text)
            if (result is JobNoteOutcome.Success) {
                _uiState.update { it.copy(showAddNote = false, noteText = "") }
            }
        }
    }

    private fun executeDeleteNote(note: JobNote) {
        viewModelScope.launch {
            deleteJobNoteUseCase(note)
        }
    }

    private fun executeSummarizeNotes(notes: List<JobNote>) {
        if (notes.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSummarizing = true, aiSummary = null) }
            val data = notes.joinToString("\n") { it.text }
            val result = generateSummaryUseCase(data)
            if (result is GeminiOutcome.Success) {
                _uiState.update { it.copy(aiSummary = result.text, isSummarizing = false) }
            } else {
                _uiState.update { it.copy(isSummarizing = false) }
            }
        }
    }

    private fun executeLinkReceipt(receipt: ReceiptItem, clientName: String) {
        viewModelScope.launch {
            linkReceiptToClientUseCase(receipt, clientName)
            _uiState.update { it.copy(showReceiptPicker = false) }
        }
    }

    private fun executeSavePhoto(uri: String, invId: String) {
        viewModelScope.launch {
            saveJobPhotoUseCase(uri, invId)
        }
    }
}
