package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.InvoicePaymentStatus
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.PowerPayConnectionMode
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalDraft
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPhase
import com.fordham.toolbelt.domain.model.stripe.StripePaymentMode
import kotlinx.coroutines.flow.StateFlow

data class CardTerminalUiState(
    val draft: CardTerminalDraft = CardTerminalDraft(),
    val phase: CardTerminalPhase = CardTerminalPhase.Idle,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isProcessing: Boolean = false
)

data class PaymentUiState(
    val requests: List<InvoicePaymentRequest> = emptyList(),
    val latestRequest: InvoicePaymentRequest? = null,
    val errorMessage: String? = null,
    val openUrlOnce: String? = null,
    val isCreatingPaymentRequest: Boolean = false,
    val lastReceiptUrl: String? = null,
    val lastPaidInvoiceId: String? = null,
    val connectionMode: PowerPayConnectionMode = PowerPayConnectionMode.Demo,
    val isGeneratingCheckoutSession: Boolean = false,
    val paymentSuccessMessage: String? = null,
    val activeCheckoutUrl: String? = null,
    val checkoutLinkCanPay: Boolean = true,
    val checkoutLinkMessage: String? = null,
    val isResolvingCheckoutLink: Boolean = false
) {
    val pendingCount: Int get() = requests.count { it.status != InvoicePaymentStatus.Paid }
    val totalRequested: Double get() = requests.sumOf { it.requestedAmount.value }
    val isLivePowerPay: Boolean get() = connectionMode is PowerPayConnectionMode.Live
    val connectionBanner: String
        get() = when (val mode = connectionMode) {
            PowerPayConnectionMode.Demo -> "Demo mode — configure PowerPay credentials for live Stellar checkout."
            is PowerPayConnectionMode.Live -> "Stellar PowerPay · ${mode.presetLabel} · ${mode.environmentLabel}"
        }
}

class PaymentViewModel(
    val invoicePaymentViewModel: InvoicePaymentViewModel,
    val stripeConnectViewModel: StripeConnectViewModel,
    val stripeTerminalViewModel: StripeTerminalViewModel
) : ViewModel() {

    val uiState: StateFlow<PaymentUiState> = invoicePaymentViewModel.uiState
    val cardTerminal: StateFlow<CardTerminalUiState> = stripeTerminalViewModel.cardTerminal
    val stripePaymentMode: StripePaymentMode = stripeConnectViewModel.stripePaymentMode

    fun requestDeposit(invoice: Invoice, provider: PaymentProviderType) {
        stripeConnectViewModel.requestDeposit(invoice, provider)
    }

    fun requestFullPayment(invoice: Invoice, provider: PaymentProviderType) {
        stripeConnectViewModel.requestFullPayment(invoice, provider)
    }

    fun clearLatestRequest() {
        invoicePaymentViewModel.clearLatestRequest()
    }

    fun refreshActiveCheckoutLink() {
        stripeConnectViewModel.refreshActiveCheckoutLink()
    }

    fun regenerateCheckoutLink() {
        stripeConnectViewModel.regenerateCheckoutLink()
    }

    fun refreshPendingStripeCheckout() {
        invoicePaymentViewModel.refreshPendingStripeCheckout()
    }

    fun clearError() {
        invoicePaymentViewModel.clearError()
    }

    fun clearPaymentSuccessMessage() {
        invoicePaymentViewModel.clearPaymentSuccessMessage()
    }

    fun createRequest(
        invoice: Invoice,
        type: PaymentRequestType,
        provider: PaymentProviderType,
        onFinished: () -> Unit = {}
    ) {
        stripeConnectViewModel.createRequest(invoice, type, provider, onFinished)
    }

    fun consumeOpenUrl() {
        invoicePaymentViewModel.consumeOpenUrl()
    }

    fun updateCardTerminalDraft(draft: CardTerminalDraft) {
        stripeTerminalViewModel.updateCardTerminalDraft(draft)
    }

    fun resetCardTerminal() {
        stripeTerminalViewModel.resetCardTerminal()
    }

    fun chargeStripePaymentSheet(invoice: Invoice, type: PaymentRequestType, onSuccess: () -> Unit) {
        stripeTerminalViewModel.chargeStripePaymentSheet(invoice, type, onSuccess)
    }

    fun collectTapToPay(invoice: Invoice, type: PaymentRequestType, onSuccess: () -> Unit, onError: (String) -> Unit) {
        stripeTerminalViewModel.collectTapToPay(invoice, type, onSuccess, onError)
    }

    fun collectBluetoothReader(
        invoice: Invoice,
        type: PaymentRequestType,
        onSuccess: () -> Unit,
        onPremiumRequired: () -> Unit,
        onError: (String) -> Unit
    ) {
        stripeTerminalViewModel.collectBluetoothReader(invoice, type, onSuccess, onPremiumRequired, onError)
    }

    fun chargeCardTerminal(invoice: Invoice, type: PaymentRequestType, onSuccess: () -> Unit) {
        stripeTerminalViewModel.chargeCardTerminal(invoice, type, onSuccess)
    }

    fun generateAndLaunchStripeCheckout(invoice: Invoice, type: PaymentRequestType) {
        stripeConnectViewModel.generateAndLaunchStripeCheckout(invoice, type)
    }
}
