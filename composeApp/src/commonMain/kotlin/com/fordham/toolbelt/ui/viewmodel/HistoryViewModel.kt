package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.GeminiOutcome
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.usecase.ComposeAiInvoiceReminderUseCase
import com.fordham.toolbelt.domain.usecase.ReminderTone
import com.fordham.toolbelt.domain.usecase.ReminderChannel
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@kotlinx.serialization.Serializable
private data class ReminderPayloadDto(
    val subject: String = "",
    val body: String = ""
)

data class HistoryUiState(
    val invoiceToDelete: Invoice? = null,
    val searchQuery: String = "",
    val showPaidOnly: Boolean = false,
    val showReminderSheet: Boolean = false,
    val reminderInvoice: Invoice? = null,
    val reminderTone: ReminderTone = ReminderTone.FRIENDLY,
    val reminderChannel: ReminderChannel = ReminderChannel.SMS,
    val generatedSubject: String = "",
    val generatedBody: String = "",
    val isGeneratingReminder: Boolean = false,
    val reminderError: String? = null
)

class HistoryViewModel(
    private val invoiceRepository: InvoiceRepository,
    private val composeAiInvoiceReminderUseCase: ComposeAiInvoiceReminderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val allInvoices: Flow<List<Invoice>> = invoiceRepository.allInvoices

    val filteredInvoices = combine(allInvoices, _uiState) { invoices, state ->
        invoices.filter { 
            (it.clientName.contains(state.searchQuery, ignoreCase = true) || it.itemsSummary.contains(state.searchQuery, ignoreCase = true)) &&
            (!state.showPaidOnly || it.isPaid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setInvoiceToDelete(invoice: Invoice?) { _uiState.update { it.copy(invoiceToDelete = invoice) } }
    
    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch { invoiceRepository.deleteInvoice(invoice) }
    }

    fun updateInvoice(invoice: Invoice) {
        viewModelScope.launch { invoiceRepository.updateInvoice(invoice) }
    }

    fun onSearchQueryChange(query: String) { _uiState.update { it.copy(searchQuery = query) } }
    fun onShowPaidOnlyChange(paidOnly: Boolean) { _uiState.update { it.copy(showPaidOnly = paidOnly) } }
    
    fun convertEstimateToInvoice(estimate: Invoice) {
        viewModelScope.launch {
            invoiceRepository.updateInvoice(estimate.copy(isEstimate = false))
        }
    }

    fun markInvoicePaid(invoice: Invoice) {
        updateInvoice(invoice.copy(isPaid = true))
    }

    fun selectInvoiceForReminder(invoice: Invoice?) {
        _uiState.update {
            it.copy(
                reminderInvoice = invoice,
                showReminderSheet = invoice != null,
                generatedSubject = "",
                generatedBody = "",
                reminderError = null
            )
        }
    }

    fun updateReminderTone(tone: ReminderTone) {
        _uiState.update { it.copy(reminderTone = tone) }
    }

    fun updateReminderChannel(channel: ReminderChannel) {
        _uiState.update { it.copy(reminderChannel = channel) }
    }

    fun updateGeneratedText(subject: String, body: String) {
        _uiState.update { it.copy(generatedSubject = subject, generatedBody = body) }
    }

    fun generateReminder(contractorName: String?, paymentLink: String?) {
        val invoice = _uiState.value.reminderInvoice ?: return
        _uiState.update { it.copy(isGeneratingReminder = true, reminderError = null) }
        viewModelScope.launch {
            val outcome = composeAiInvoiceReminderUseCase(
                invoice = invoice,
                tone = _uiState.value.reminderTone,
                channel = _uiState.value.reminderChannel,
                contractorName = contractorName,
                paymentLink = paymentLink
            )
            _uiState.update { state ->
                when (outcome) {
                    is GeminiOutcome.Success -> {
                        val cleanJson = com.fordham.toolbelt.util.AiUtil.cleanJson(outcome.text)
                        try {
                            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                            val reminder = json.decodeFromString<ReminderPayloadDto>(cleanJson)
                            state.copy(
                                isGeneratingReminder = false,
                                generatedSubject = reminder.subject,
                                generatedBody = reminder.body
                            )
                        } catch (e: Exception) {
                            state.copy(
                                isGeneratingReminder = false,
                                reminderError = "Failed to parse AI reminder: ${e.message}"
                            )
                        }
                    }
                    is GeminiOutcome.Failure -> {
                        state.copy(
                            isGeneratingReminder = false,
                            reminderError = outcome.error.value
                        )
                    }
                }
            }
        }
    }
}
