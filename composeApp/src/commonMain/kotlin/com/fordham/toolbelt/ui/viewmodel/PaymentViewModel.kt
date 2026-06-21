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
import com.fordham.toolbelt.util.UiMessageKeys
import com.fordham.toolbelt.domain.usecase.stripe.ProcessTapToPayUseCase
import com.fordham.toolbelt.domain.repository.StripeCheckoutRepository
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.deeplink.DeepLinkDispatcher
import com.fordham.toolbelt.domain.deeplink.DeepLinkEvent
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.usesStripeRail
import com.fordham.toolbelt.domain.repository.PaymentRepository
import com.fordham.toolbelt.domain.usecase.stripe.PollStripeInvoicePaymentStatusUseCase
import com.fordham.toolbelt.domain.usecase.stripe.VerifyStripeCheckoutOutcome
import com.fordham.toolbelt.domain.usecase.stripe.VerifyStripeCheckoutSessionUseCase
import com.fordham.toolbelt.domain.usecase.stripe.ResolveStripeCheckoutLinkUseCase
import com.fordham.toolbelt.domain.usecase.stripe.ResolveStripeCheckoutLinkOutcome
import com.fordham.toolbelt.domain.payment.stripe.StripeCheckoutUrlParser
import kotlinx.datetime.Clock
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
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val verifyStripeCheckoutSessionUseCase: VerifyStripeCheckoutSessionUseCase,
    private val pollStripeInvoicePaymentStatusUseCase: PollStripeInvoicePaymentStatusUseCase,
    private val resolveStripeCheckoutLinkUseCase: ResolveStripeCheckoutLinkUseCase
) : ViewModel() {

    val stripePaymentMode: StripePaymentMode = getStripePaymentModeUseCase()
    private val connectionMode = getPowerPayConnectionModeUseCase()
    private val transientState = MutableStateFlow(PaymentUiState(connectionMode = connectionMode))
    private val _cardTerminal = MutableStateFlow(CardTerminalUiState())
    val cardTerminal: StateFlow<CardTerminalUiState> = _cardTerminal.asStateFlow()
    private var pendingCheckoutSessionId: String? = null
    private var pendingCheckoutInvoiceId: String? = null
    private var pendingCheckoutContractorId: String? = null

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
            paymentSuccessMessage = transient.paymentSuccessMessage,
            activeCheckoutUrl = transient.activeCheckoutUrl,
            checkoutLinkCanPay = transient.checkoutLinkCanPay,
            checkoutLinkMessage = transient.checkoutLinkMessage,
            isResolvingCheckoutLink = transient.isResolvingCheckoutLink
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
                pollPendingStripePayments(showToast = false)
                val hasPending = uiState.value.requests.any {
                    it.status == InvoicePaymentStatus.Pending || it.status == InvoicePaymentStatus.Requested
                } || pendingCheckoutSessionId != null
                delay(if (hasPending) POLL_ACTIVE_MS else POLL_IDLE_MS)
            }
        }
        viewModelScope.launch {
            deepLinkDispatcher.events.collect { event ->
                when (event) {
                    is DeepLinkEvent.PaymentSuccess -> {
                        viewModelScope.launch {
                            handlePaymentSuccessReturn(
                                invoiceId = event.invoiceId,
                                sessionId = event.sessionId,
                                contractorUserId = event.contractorUserId
                            )
                        }
                    }
                    is DeepLinkEvent.PaymentCancelled -> {
                        updateTransient(
                            isGeneratingCheckoutSession = false,
                            errorMessage = UiMessageKeys.PAYMENT_CANCELLED,
                            paymentSuccessMessage = null
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
        viewModelScope.launch {
            refreshPendingStripeCheckoutInternal()
            updateTransient(
                latestRequest = null,
                activeCheckoutUrl = null,
                checkoutLinkCanPay = true,
                checkoutLinkMessage = null,
                isResolvingCheckoutLink = false
            )
        }
    }

    fun refreshActiveCheckoutLink() {
        uiState.value.latestRequest?.let { resolveActiveCheckoutLink(it) }
    }

    fun regenerateCheckoutLink() {
        val request = uiState.value.latestRequest ?: return
        viewModelScope.launch {
            val invoice = invoiceRepository.getInvoiceById(request.invoiceId) ?: return@launch
            createRequest(invoice, request.type, request.provider)
        }
    }

    fun refreshPendingStripeCheckout() {
        viewModelScope.launch {
            refreshPendingStripeCheckoutInternal()
        }
    }

    fun clearError() {
        updateTransient(errorMessage = null)
    }

    fun clearPaymentSuccessMessage() {
        updateTransient(paymentSuccessMessage = null)
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
                is PaymentRequestOutcome.Success -> {
                    rememberPendingStripeCheckout(outcome.request)
                    updateTransient(
                        latestRequest = outcome.request,
                        errorMessage = null,
                        openUrlOnce = null,
                        isCreatingPaymentRequest = false
                    )
                    resolveActiveCheckoutLink(outcome.request)
                }
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
                    UiMessageKeys.BLUETOOTH_READERS_NOT_ENABLED
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
                            successMessage = UiMessageKeys.paidCardTerminal(outcome.request.formattedAmount),
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

    private suspend fun handlePaymentSuccessReturn(
        invoiceId: String,
        sessionId: String?,
        contractorUserId: String? = null
    ) {
        updateTransient(isGeneratingCheckoutSession = false, errorMessage = null)
        val contractorId = contractorUserId?.takeIf { it.isNotBlank() }
            ?: pendingCheckoutContractorId
            ?: authRepository.currentUser.first()?.id?.value

        if (!sessionId.isNullOrBlank()) {
            repeat(4) { attempt ->
                when (
                    val outcome = verifyStripeCheckoutSessionUseCase(sessionId, contractorId)
                ) {
                    is VerifyStripeCheckoutOutcome.Verified -> {
                        clearPendingCheckout()
                        applyStripePaymentSuccess(outcome.invoiceId)
                        return
                    }
                    is VerifyStripeCheckoutOutcome.Failure -> {
                        val message = outcome.error.value
                        if (message.contains("Not found", ignoreCase = true)) {
                            updateTransient(
                                errorMessage = "Payment sync unavailable — redeploy stripe-payment-api on Supabase."
                            )
                        } else {
                            updateTransient(errorMessage = message)
                        }
                        return
                    }
                    VerifyStripeCheckoutOutcome.NotPaid -> {
                        if (attempt < 3) delay(1_500)
                    }
                    VerifyStripeCheckoutOutcome.BackendNotConfigured -> return
                }
            }
        }

        val targetInvoiceId = invoiceId.takeIf { it.isNotBlank() }
            ?: pendingCheckoutInvoiceId
            ?: return
        if (isInvoicePaidOnBackend(targetInvoiceId)) {
            clearPendingCheckout()
            applyStripePaymentSuccess(targetInvoiceId)
        }
    }

    private suspend fun refreshPendingStripeCheckoutInternal() {
        val sessionId = pendingCheckoutSessionId
        val invoiceId = pendingCheckoutInvoiceId.orEmpty()
        if (!sessionId.isNullOrBlank()) {
            handlePaymentSuccessReturn(
                invoiceId = invoiceId,
                sessionId = sessionId,
                contractorUserId = pendingCheckoutContractorId
            )
        } else {
            pollPendingStripePayments(showToast = true)
        }
    }

    private fun rememberPendingStripeCheckout(request: InvoicePaymentRequest) {
        if (!request.provider.usesStripeRail) return
        val checkoutUrl = request.paymentLink.value
        if (!checkoutUrl.contains("checkout.stripe.com")) return
        pendingCheckoutSessionId = StripeCheckoutUrlParser.extractSessionId(checkoutUrl)
        pendingCheckoutInvoiceId = request.invoiceId.value
        viewModelScope.launch {
            pendingCheckoutContractorId = authRepository.currentUser.first()?.id?.value
        }
    }

    private fun resolveActiveCheckoutLink(request: InvoicePaymentRequest) {
        if (!request.provider.usesStripeRail ||
            !StripeCheckoutUrlParser.isHostedCheckoutUrl(request.paymentLink.value)
        ) {
            updateTransient(
                activeCheckoutUrl = request.paymentLink.value,
                checkoutLinkCanPay = true,
                checkoutLinkMessage = null,
                isResolvingCheckoutLink = false
            )
            return
        }

        val sessionId = StripeCheckoutUrlParser.extractSessionId(request.paymentLink.value)
        if (sessionId.isNullOrBlank()) {
            updateTransient(
                activeCheckoutUrl = request.paymentLink.value,
                checkoutLinkCanPay = true,
                checkoutLinkMessage = null,
                isResolvingCheckoutLink = false
            )
            return
        }

        viewModelScope.launch {
            updateTransient(isResolvingCheckoutLink = true, checkoutLinkMessage = null)
            when (val outcome = resolveStripeCheckoutLinkUseCase(sessionId)) {
                is ResolveStripeCheckoutLinkOutcome.Resolved -> {
                    when {
                        outcome.canPay && !outcome.checkoutUrl.isNullOrBlank() -> {
                            updateTransient(
                                activeCheckoutUrl = outcome.checkoutUrl,
                                checkoutLinkCanPay = true,
                                checkoutLinkMessage = null,
                                isResolvingCheckoutLink = false
                            )
                        }
                        outcome.paid -> {
                            updateTransient(
                                activeCheckoutUrl = null,
                                checkoutLinkCanPay = false,
                                checkoutLinkMessage = UiMessageKeys.CHECKOUT_LINK_ALREADY_PAID,
                                isResolvingCheckoutLink = false
                            )
                        }
                        else -> {
                            updateTransient(
                                activeCheckoutUrl = null,
                                checkoutLinkCanPay = false,
                                checkoutLinkMessage = UiMessageKeys.CHECKOUT_LINK_EXPIRED,
                                isResolvingCheckoutLink = false
                            )
                        }
                    }
                }
                is ResolveStripeCheckoutLinkOutcome.Failure -> {
                    updateTransient(
                        activeCheckoutUrl = request.paymentLink.value,
                        checkoutLinkCanPay = true,
                        checkoutLinkMessage = outcome.error.value,
                        isResolvingCheckoutLink = false
                    )
                }
            }
        }
    }

    private fun clearPendingCheckout() {
        pendingCheckoutSessionId = null
        pendingCheckoutInvoiceId = null
        pendingCheckoutContractorId = null
    }

    private suspend fun pollPendingStripePayments(showToast: Boolean) {
        val fromLedger = uiState.value.requests
            .filter {
                it.provider.usesStripeRail &&
                    (it.status == InvoicePaymentStatus.Pending || it.status == InvoicePaymentStatus.Requested)
            }
            .map { it.invoiceId }
        val tracked = pendingCheckoutInvoiceId?.let { InvoiceId(it) }
        val pendingStripeInvoiceIds = (fromLedger + listOfNotNull(tracked)).distinct()
        if (pendingStripeInvoiceIds.isEmpty()) return

        val paidIds = pollStripeInvoicePaymentStatusUseCase(pendingStripeInvoiceIds)
        paidIds.forEach { paidInvoiceId ->
            applyStripePaymentSuccess(paidInvoiceId.value, showToast = showToast)
        }
    }

    private suspend fun isInvoicePaidOnBackend(invoiceId: String): Boolean {
        val paidIds = pollStripeInvoicePaymentStatusUseCase(listOf(InvoiceId(invoiceId)))
        return paidIds.contains(InvoiceId(invoiceId))
    }

    private suspend fun applyStripePaymentSuccess(invoiceId: String, showToast: Boolean = true) {
        if (invoiceId.isBlank()) return
        val existingInvoice = invoiceRepository.getInvoiceById(InvoiceId(invoiceId))
        val wasAlreadyPaid = existingInvoice?.isPaid == true && existingInvoice.isEstimate == false
        val paidAt = Clock.System.now().toEpochMilliseconds()
        paymentRepository.markInvoicePaid(InvoiceId(invoiceId), paidAt, null, null)
        existingInvoice?.let { invoice ->
            val updated = invoice.copy(isPaid = true, isEstimate = false)
            if (updated != invoice) {
                invoiceRepository.updateInvoice(updated)
            }
        }
        updateTransient(
            lastPaidInvoiceId = invoiceId,
            paymentSuccessMessage = if (showToast && !wasAlreadyPaid) {
                UiMessageKeys.PAYMENT_SUCCESS
            } else {
                uiState.value.paymentSuccessMessage
            },
            errorMessage = null
        )
        clearPendingCheckout()
    }

    fun generateAndLaunchStripeCheckout(invoice: Invoice, type: PaymentRequestType) {
        viewModelScope.launch {
            updateTransient(isGeneratingCheckoutSession = true, errorMessage = null, paymentSuccessMessage = null)
            val amount = when (type) {
                PaymentRequestType.Deposit ->
                    invoice.depositAmount.value.takeIf { it > 0.0 } ?: (invoice.totalAmount.value * 0.30)
                PaymentRequestType.FullBalance -> invoice.totalAmount.value
            }
            val amountInCents = (amount * 100).toLong()
            val contractorId = authRepository.currentUser.first()?.id?.value ?: "anonymous"

            when (val outcome = stripeCheckoutRepository.generateFallbackPaymentLink(invoice.id, amountInCents, contractorId)) {
                StripeCheckoutSessionOutcome.NotConfigured -> {
                    updateTransient(
                        isGeneratingCheckoutSession = false,
                        errorMessage = UiMessageKeys.STRIPE_CONNECT_NOT_CONFIGURED
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
        paymentSuccessMessage: String? = uiState.value.paymentSuccessMessage,
        activeCheckoutUrl: String? = uiState.value.activeCheckoutUrl,
        checkoutLinkCanPay: Boolean = uiState.value.checkoutLinkCanPay,
        checkoutLinkMessage: String? = uiState.value.checkoutLinkMessage,
        isResolvingCheckoutLink: Boolean = uiState.value.isResolvingCheckoutLink
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
                paymentSuccessMessage = paymentSuccessMessage,
                activeCheckoutUrl = activeCheckoutUrl,
                checkoutLinkCanPay = checkoutLinkCanPay,
                checkoutLinkMessage = checkoutLinkMessage,
                isResolvingCheckoutLink = isResolvingCheckoutLink
            )
            if (next == current) current else next
        }
    }

    private companion object {
        const val POLL_ACTIVE_MS = 5_000L
        const val POLL_IDLE_MS = 15_000L
    }
}
