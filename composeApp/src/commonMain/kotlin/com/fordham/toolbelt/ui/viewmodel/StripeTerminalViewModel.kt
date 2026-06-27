package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalDraft
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPaymentOutcome
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPhase
import com.fordham.toolbelt.domain.repository.CardTerminalPaymentGateway
import com.fordham.toolbelt.domain.usecase.ProcessCardTerminalPaymentUseCase
import com.fordham.toolbelt.domain.model.stripe.BluetoothReaderOutcome
import com.fordham.toolbelt.domain.model.stripe.StripeCardCollectOutcome
import com.fordham.toolbelt.domain.model.stripe.TapToPayOutcome
import com.fordham.toolbelt.domain.usecase.stripe.ProcessBluetoothReaderPaymentUseCase
import com.fordham.toolbelt.domain.usecase.stripe.ProcessStripePaymentSheetUseCase
import com.fordham.toolbelt.domain.usecase.stripe.ProcessTapToPayUseCase
import com.fordham.toolbelt.util.UiMessageKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StripeTerminalViewModel(
    private val invoicePaymentViewModel: InvoicePaymentViewModel,
    private val processCardTerminalPaymentUseCase: ProcessCardTerminalPaymentUseCase,
    private val cardTerminalGateway: CardTerminalPaymentGateway,
    private val processStripePaymentSheetUseCase: ProcessStripePaymentSheetUseCase,
    private val processTapToPayUseCase: ProcessTapToPayUseCase,
    private val processBluetoothReaderPaymentUseCase: ProcessBluetoothReaderPaymentUseCase
) : ViewModel() {

    private val _cardTerminal = MutableStateFlow(CardTerminalUiState())
    val cardTerminal: StateFlow<CardTerminalUiState> = _cardTerminal.asStateFlow()

    init {
        viewModelScope.launch {
            cardTerminalGateway.phase.collect { phase ->
                _cardTerminal.update {
                    it.copy(
                        phase = phase,
                        isProcessing = phase in setOf(
                            CardTerminalPhase.SecuringConnection,
                            CardTerminalPhase.Verifying,
                            CardTerminalPhase.Settling
                        )
                    )
                }
            }
        }
    }

    fun updateCardTerminalDraft(draft: CardTerminalDraft) {
        _cardTerminal.update { it.copy(draft = draft, errorMessage = null) }
    }

    fun resetCardTerminal() {
        cardTerminalGateway.resetPhase()
        _cardTerminal.value = CardTerminalUiState()
    }

    fun chargeStripePaymentSheet(invoice: Invoice, type: PaymentRequestType, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _cardTerminal.update { it.copy(errorMessage = null, successMessage = null, isProcessing = true) }
            when (val outcome = processStripePaymentSheetUseCase(invoice, type)) {
                StripeCardCollectOutcome.Cancelled ->
                    _cardTerminal.update { it.copy(isProcessing = false) }
                is StripeCardCollectOutcome.Failure ->
                    _cardTerminal.update { it.copy(errorMessage = outcome.error.value, isProcessing = false) }
                is StripeCardCollectOutcome.Success -> {
                    invoicePaymentViewModel.updateTransient(
                        latestRequest = outcome.request,
                        lastPaidInvoiceId = invoice.id.value,
                        clearErrorMessage = true
                    )
                    _cardTerminal.update {
                        it.copy(
                            successMessage = UiMessageKeys.paidSecureCheckout(outcome.request.formattedAmount),
                            isProcessing = false
                        )
                    }
                    onSuccess()
                }
            }
        }
    }

    fun collectTapToPay(invoice: Invoice, type: PaymentRequestType, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val outcome = processTapToPayUseCase(invoice, type)) {
                TapToPayOutcome.Cancelled -> Unit
                is TapToPayOutcome.Failure -> onError(outcome.error.value)
                is TapToPayOutcome.Success -> {
                    invoicePaymentViewModel.updateTransient(
                        latestRequest = outcome.request,
                        lastPaidInvoiceId = invoice.id.value,
                        clearErrorMessage = true
                    )
                    onSuccess()
                }
            }
        }
    }

    fun collectBluetoothReader(
        invoice: Invoice,
        type: PaymentRequestType,
        onSuccess: () -> Unit,
        onPremiumRequired: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val outcome = processBluetoothReaderPaymentUseCase(invoice, type)) {
                BluetoothReaderOutcome.Cancelled -> Unit
                BluetoothReaderOutcome.PremiumRequired -> onPremiumRequired()
                BluetoothReaderOutcome.ReaderNotAvailable -> onError(
                    UiMessageKeys.BLUETOOTH_READERS_NOT_ENABLED
                )
                is BluetoothReaderOutcome.Failure -> onError(outcome.error.value)
                is BluetoothReaderOutcome.Success -> {
                    invoicePaymentViewModel.updateTransient(
                        latestRequest = outcome.request,
                        lastPaidInvoiceId = invoice.id.value,
                        clearErrorMessage = true
                    )
                    onSuccess()
                }
            }
        }
    }

    fun chargeCardTerminal(invoice: Invoice, type: PaymentRequestType, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _cardTerminal.update { it.copy(errorMessage = null, successMessage = null) }
            when (val outcome = processCardTerminalPaymentUseCase(invoice, type, _cardTerminal.value.draft)) {
                is CardTerminalPaymentOutcome.Failure ->
                    _cardTerminal.update { it.copy(errorMessage = outcome.error.value, isProcessing = false) }
                is CardTerminalPaymentOutcome.Success -> {
                    invoicePaymentViewModel.updateTransient(
                        latestRequest = outcome.request,
                        lastPaidInvoiceId = invoice.id.value,
                        clearErrorMessage = true
                    )
                    _cardTerminal.update {
                        it.copy(
                            successMessage = UiMessageKeys.paidCardTerminal(outcome.request.formattedAmount),
                            isProcessing = false
                        )
                    }
                    onSuccess()
                }
            }
        }
    }
}
