package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.InvoicePaymentStatus
import com.fordham.toolbelt.domain.model.PaymentLedgerOutcome
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestOutcome
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.PowerPayConnectionMode
import com.fordham.toolbelt.domain.model.PowerPayEvent
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalDraft
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPaymentOutcome
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPhase
import com.fordham.toolbelt.domain.repository.CardTerminalPaymentGateway
import com.fordham.toolbelt.domain.usecase.CreatePaymentRequestUseCase
import com.fordham.toolbelt.domain.usecase.GetPaymentLedgerUseCase
import com.fordham.toolbelt.domain.usecase.GetPowerPayConnectionModeUseCase
import com.fordham.toolbelt.domain.usecase.PollPowerPayClientEventsUseCase
import com.fordham.toolbelt.domain.model.stripe.BluetoothReaderOutcome
import com.fordham.toolbelt.domain.model.stripe.StripeCardCollectOutcome
import com.fordham.toolbelt.domain.model.stripe.StripePaymentMode
import com.fordham.toolbelt.domain.model.stripe.TapToPayOutcome
import com.fordham.toolbelt.domain.usecase.ProcessCardTerminalPaymentUseCase
import com.fordham.toolbelt.domain.usecase.stripe.GetStripePaymentModeUseCase
import com.fordham.toolbelt.domain.usecase.stripe.ProcessBluetoothReaderPaymentUseCase
import com.fordham.toolbelt.domain.usecase.stripe.ProcessStripePaymentSheetUseCase
import com.fordham.toolbelt.domain.usecase.stripe.ProcessTapToPayUseCase
import com.fordham.toolbelt.domain.repository.StripeCheckoutRepository
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.deeplink.DeepLinkDispatcher
import com.fordham.toolbelt.domain.deeplink.DeepLinkEvent
import com.fordham.toolbelt.domain.model.stripe.StripeCheckoutSessionOutcome
import com.fordham.toolbelt.util.PlatformActions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    /** One-shot URL to open (e.g. Stripe Connect onboarding after a 409). */
    val openUrlOnce: String? = null,
    val isCreatingPaymentRequest: Boolean = false,
    val lastReceiptUrl: String? = null,
    val lastPaidInvoiceId: String? = null,
    val connectionMode: PowerPayConnectionMode = PowerPayConnectionMode.Demo,
    val isGeneratingCheckoutSession: Boolean = false,
    val paymentSuccessMessage: String? = null
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
    getPaymentLedgerUseCase: GetPaymentLedgerUseCase,
    private val createPaymentRequestUseCase: CreatePaymentRequestUseCase,
    private val pollPowerPayClientEventsUseCase: PollPowerPayClientEventsUseCase,
    getPowerPayConnectionModeUseCase: GetPowerPayConnectionModeUseCase,
    private val processCardTerminalPaymentUseCase: ProcessCardTerminalPaymentUseCase,
    private val cardTerminalGateway: CardTerminalPaymentGateway,
    getStripePaymentModeUseCase: GetStripePaymentModeUseCase,
    private val processStripePaymentSheetUseCase: ProcessStripePaymentSheetUseCase,
    private val processTapToPayUseCase: ProcessTapToPayUseCase,
    private val processBluetoothReaderPaymentUseCase: ProcessBluetoothReaderPaymentUseCase,
    private val stripeCheckoutRepository: StripeCheckoutRepository,
    private val deepLinkDispatcher: DeepLinkDispatcher,
    private val platformActions: PlatformActions,
    private val authRepository: AuthRepository,
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    val stripePaymentMode: StripePaymentMode = getStripePaymentModeUseCase()
    private val connectionMode = getPowerPayConnectionModeUseCase()
    private val transientState = MutableStateFlow(PaymentUiState(connectionMode = connectionMode))
    private val _cardTerminal = MutableStateFlow(CardTerminalUiState())
    val cardTerminal: StateFlow<CardTerminalUiState> = _cardTerminal.asStateFlow()

    val uiState: StateFlow<PaymentUiState> = combine(
        getPaymentLedgerUseCase.ledger
            .map { outcome ->
                when (outcome) {
                    is PaymentLedgerOutcome.Success -> PaymentUiState(
                        requests = outcome.requests,
                        connectionMode = connectionMode
                    )
                    is PaymentLedgerOutcome.Failure -> PaymentUiState(
                        errorMessage = outcome.error.value,
                        connectionMode = connectionMode
                    )
                }
            }
            .distinctUntilChanged(),
        transientState
    ) { ledgerState, transient ->
        ledgerState.copy(
            latestRequest = transient.latestRequest,
            errorMessage = transient.errorMessage ?: ledgerState.errorMessage,
            openUrlOnce = transient.openUrlOnce,
            isCreatingPaymentRequest = transient.isCreatingPaymentRequest,
            lastReceiptUrl = transient.lastReceiptUrl,
            lastPaidInvoiceId = transient.lastPaidInvoiceId,
            connectionMode = connectionMode,
            isGeneratingCheckoutSession = transient.isGeneratingCheckoutSession,
            paymentSuccessMessage = transient.paymentSuccessMessage
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaymentUiState(connectionMode = connectionMode))

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
        viewModelScope.launch {
            while (isActive) {
                runCatching { pollPowerPayClientEventsUseCase() }
                    .getOrNull()
                    ?.forEach { event -> applyClientEventUi(event) }
                val hasPending = uiState.value.requests.any {
                    it.status == InvoicePaymentStatus.Pending || it.status == InvoicePaymentStatus.Requested
                }
                delay(if (hasPending) POLL_ACTIVE_MS else POLL_IDLE_MS)
            }
        }
        viewModelScope.launch {
            deepLinkDispatcher.events.collect { event ->
                when (event) {
                    is DeepLinkEvent.PaymentSuccess -> {
                        updateTransient(
                            isGeneratingCheckoutSession = false,
                            errorMessage = null,
                            lastPaidInvoiceId = event.invoiceId,
                            paymentSuccessMessage = "Secure payment succeeded! Invoice is marked paid."
                        )
                        viewModelScope.launch {
                            val invoice = invoiceRepository.getInvoiceById(com.fordham.toolbelt.domain.model.InvoiceId(event.invoiceId))
                            if (invoice != null) {
                                invoiceRepository.updateInvoice(invoice.copy(isPaid = true))
                            }
                        }
                    }
                    is DeepLinkEvent.PaymentCancelled -> {
                        updateTransient(
                            isGeneratingCheckoutSession = false,
                            errorMessage = "Payment checkout was cancelled."
                        )
                    }
                }
            }
        }
    }

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

    fun createRequest(
        invoice: Invoice,
        type: PaymentRequestType,
        provider: PaymentProviderType,
        onFinished: () -> Unit = {}
    ) {
        viewModelScope.launch {
            updateTransient(isCreatingPaymentRequest = true, errorMessage = null, openUrlOnce = null)
            when (val outcome = createPaymentRequestUseCase(invoice, type, provider)) {
                is PaymentRequestOutcome.Success ->
                    updateTransient(
                        latestRequest = outcome.request,
                        errorMessage = null,
                        openUrlOnce = null,
                        isCreatingPaymentRequest = false
                    )
                is PaymentRequestOutcome.Failure ->
                    updateTransient(
                        errorMessage = outcome.error.value,
                        openUrlOnce = outcome.actionUrl,
                        isCreatingPaymentRequest = false
                    )
            }
            onFinished()
        }
    }

    fun consumeOpenUrl() {
        updateTransient(openUrlOnce = null)
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
                    updateTransient(
                        latestRequest = outcome.request,
                        lastPaidInvoiceId = invoice.id.value,
                        errorMessage = null
                    )
                    _cardTerminal.update {
                        it.copy(
                            successMessage = "Paid ${outcome.request.formattedAmount} via secure checkout.",
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
                    updateTransient(
                        latestRequest = outcome.request,
                        lastPaidInvoiceId = invoice.id.value,
                        errorMessage = null
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
                    "Bluetooth card readers are not enabled in this build. Use Tap to Pay or Card Terminal."
                )
                is BluetoothReaderOutcome.Failure -> onError(outcome.error.value)
                is BluetoothReaderOutcome.Success -> {
                    updateTransient(
                        latestRequest = outcome.request,
                        lastPaidInvoiceId = invoice.id.value,
                        errorMessage = null
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
                    updateTransient(
                        latestRequest = outcome.request,
                        lastPaidInvoiceId = invoice.id.value,
                        errorMessage = null
                    )
                    _cardTerminal.update {
                        it.copy(
                            successMessage = "Paid ${outcome.request.formattedAmount} — invoice marked paid.",
                            isProcessing = false
                        )
                    }
                    onSuccess()
                }
            }
        }
    }

    private fun applyClientEventUi(event: PowerPayEvent) {
        when (event) {
            is PowerPayEvent.InvoicePaid -> updateTransient(
                lastReceiptUrl = event.receiptUrl?.value,
                lastPaidInvoiceId = event.invoiceId.value,
                errorMessage = null
            )
            is PowerPayEvent.DepositReceived,
            is PowerPayEvent.MilestoneReleased -> Unit
        }
    }

    fun generateAndLaunchStripeCheckout(invoice: Invoice, type: PaymentRequestType) {
        viewModelScope.launch {
            updateTransient(isGeneratingCheckoutSession = true, errorMessage = null, paymentSuccessMessage = null)
            val amount = when (type) {
                PaymentRequestType.Deposit ->
                    invoice.depositAmount.takeIf { it > 0.0 } ?: (invoice.totalAmount * 0.30)
                PaymentRequestType.FullBalance -> invoice.totalAmount
            }
            val amountInCents = (amount * 100).toLong()
            val contractorId = authRepository.currentUser.first()?.id?.value ?: "anonymous"

            when (val outcome = stripeCheckoutRepository.generateFallbackPaymentLink(invoice.id, amountInCents, contractorId)) {
                StripeCheckoutSessionOutcome.NotConfigured -> {
                    updateTransient(
                        isGeneratingCheckoutSession = false,
                        errorMessage = "Stripe Connect backend is not configured."
                    )
                }
                is StripeCheckoutSessionOutcome.Failure -> {
                    updateTransient(
                        isGeneratingCheckoutSession = false,
                        errorMessage = outcome.error.value
                    )
                }
                is StripeCheckoutSessionOutcome.Success -> {
                    updateTransient(isGeneratingCheckoutSession = false)
                    platformActions.openUrl(outcome.checkoutUrl.value)
                }
            }
        }
    }

    private fun updateTransient(
        latestRequest: InvoicePaymentRequest? = uiState.value.latestRequest,
        errorMessage: String? = uiState.value.errorMessage,
        openUrlOnce: String? = uiState.value.openUrlOnce,
        isCreatingPaymentRequest: Boolean = uiState.value.isCreatingPaymentRequest,
        lastReceiptUrl: String? = uiState.value.lastReceiptUrl,
        lastPaidInvoiceId: String? = uiState.value.lastPaidInvoiceId,
        isGeneratingCheckoutSession: Boolean = uiState.value.isGeneratingCheckoutSession,
        paymentSuccessMessage: String? = uiState.value.paymentSuccessMessage
    ) {
        transientState.update { current ->
            val next = current.copy(
                latestRequest = latestRequest,
                errorMessage = errorMessage,
                openUrlOnce = openUrlOnce,
                isCreatingPaymentRequest = isCreatingPaymentRequest,
                lastReceiptUrl = lastReceiptUrl,
                lastPaidInvoiceId = lastPaidInvoiceId,
                isGeneratingCheckoutSession = isGeneratingCheckoutSession,
                paymentSuccessMessage = paymentSuccessMessage
            )
            if (next == current) current else next
        }
    }

    private companion object {
        const val POLL_ACTIVE_MS = 5_000L
        const val POLL_IDLE_MS = 15_000L
    }
}
