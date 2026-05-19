package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.PaymentLedgerOutcome
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestOutcome
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.usecase.CreatePaymentRequestUseCase
import com.fordham.toolbelt.domain.usecase.GetPaymentLedgerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PaymentUiState(
    val requests: List<InvoicePaymentRequest> = emptyList(),
    val latestRequest: InvoicePaymentRequest? = null,
    val errorMessage: String? = null
) {
    val pendingCount: Int get() = requests.count { it.statusLabel != "PAID" }
    val totalRequested: Double get() = requests.sumOf { it.requestedAmount.value }
}

class PaymentViewModel(
    getPaymentLedgerUseCase: GetPaymentLedgerUseCase,
    private val createPaymentRequestUseCase: CreatePaymentRequestUseCase
) : ViewModel() {

    private val transientState = MutableStateFlow(PaymentUiState())

    val uiState: StateFlow<PaymentUiState> = combine(
        getPaymentLedgerUseCase.ledger.map { outcome ->
            when (outcome) {
                is PaymentLedgerOutcome.Success -> PaymentUiState(requests = outcome.requests)
                is PaymentLedgerOutcome.Failure -> PaymentUiState(errorMessage = outcome.error.value)
            }
        },
        transientState
    ) { ledgerState, transient ->
        ledgerState.copy(
            latestRequest = transient.latestRequest,
            errorMessage = transient.errorMessage ?: ledgerState.errorMessage
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaymentUiState())

    fun requestDeposit(invoice: Invoice, provider: PaymentProviderType) {
        createRequest(invoice, PaymentRequestType.Deposit, provider)
    }

    fun requestFullPayment(invoice: Invoice, provider: PaymentProviderType) {
        createRequest(invoice, PaymentRequestType.FullBalance, provider)
    }

    fun clearLatestRequest() {
        updateTransient(latestRequest = null)
    }

    fun clearError() {
        updateTransient(errorMessage = null)
    }

    fun createRequest(invoice: Invoice, type: PaymentRequestType, provider: PaymentProviderType) {
        viewModelScope.launch {
            when (val outcome = createPaymentRequestUseCase(invoice, type, provider)) {
                is PaymentRequestOutcome.Success -> updateTransient(latestRequest = outcome.request, errorMessage = null)
                is PaymentRequestOutcome.Failure -> updateTransient(errorMessage = outcome.error.value)
            }
        }
    }

    private fun updateTransient(latestRequest: InvoicePaymentRequest? = uiState.value.latestRequest, errorMessage: String? = uiState.value.errorMessage) {
        transientState.update { it.copy(latestRequest = latestRequest, errorMessage = errorMessage) }
    }
}
