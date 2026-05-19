package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class HistoryUiState(
    val invoiceToDelete: Invoice? = null,
    val searchQuery: String = "",
    val showPaidOnly: Boolean = false
)

class HistoryViewModel(
    private val invoiceRepository: InvoiceRepository
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
}
