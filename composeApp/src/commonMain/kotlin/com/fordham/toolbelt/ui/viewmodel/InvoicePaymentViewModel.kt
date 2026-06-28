package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.InvoicePaymentStatus
import com.fordham.toolbelt.domain.model.PaymentLedgerOutcome
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.usesStripeRail
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.PaymentRepository
import com.fordham.toolbelt.domain.usecase.GetPaymentLedgerUseCase
import com.fordham.toolbelt.domain.usecase.stripe.PollStripeInvoicePaymentStatusUseCase
import com.fordham.toolbelt.domain.usecase.stripe.VerifyStripeCheckoutSessionUseCase
import com.fordham.toolbelt.domain.usecase.stripe.VerifyStripeCheckoutOutcome
import com.fordham.toolbelt.domain.payment.stripe.StripeCheckoutUrlParser
import com.fordham.toolbelt.domain.deeplink.DeepLinkDispatcher
import com.fordham.toolbelt.domain.deeplink.DeepLinkEvent
import com.fordham.toolbelt.util.UiMessageKeys
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class InvoicePaymentViewModel(
    getPaymentLedgerUseCase: GetPaymentLedgerUseCase,
    private val deepLinkDispatcher: DeepLinkDispatcher,
    private val authRepository: AuthRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val verifyStripeCheckoutSessionUseCase: VerifyStripeCheckoutSessionUseCase,
    private val pollStripeInvoicePaymentStatusUseCase: PollStripeInvoicePaymentStatusUseCase
) : ViewModel() {

    private val transientState = MutableStateFlow(PaymentUiState())
    
    var pendingCheckoutSessionId: String? = null
        private set
    var pendingCheckoutInvoiceId: String? = null
        private set
    var pendingCheckoutContractorId: String? = null
        private set

    val uiState: StateFlow<PaymentUiState> = combine(
        getPaymentLedgerUseCase.ledger
            .map { outcome ->
                when (outcome) {
                    is PaymentLedgerOutcome.Success -> PaymentUiState(
                        requests = outcome.requests
                    )
                    is PaymentLedgerOutcome.Failure -> PaymentUiState(
                        errorMessage = outcome.error.value
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
            isGeneratingCheckoutSession = transient.isGeneratingCheckoutSession,
            paymentSuccessMessage = transient.paymentSuccessMessage,
            activeCheckoutUrl = transient.activeCheckoutUrl,
            checkoutLinkCanPay = transient.checkoutLinkCanPay,
            checkoutLinkMessage = transient.checkoutLinkMessage,
            isResolvingCheckoutLink = transient.isResolvingCheckoutLink
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaymentUiState())

    init {
        viewModelScope.launch {
            while (isActive) {
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
                            clearPaymentSuccessMessage = true
                        )
                    }
                }
            }
        }
    }

    fun clearLatestRequest() {
        viewModelScope.launch {
            refreshPendingStripeCheckoutInternal()
            updateTransient(
                clearLatestRequest = true,
                clearActiveCheckoutUrl = true,
                checkoutLinkCanPay = true,
                clearCheckoutLinkMessage = true,
                isResolvingCheckoutLink = false
            )
        }
    }

    fun refreshPendingStripeCheckout() {
        viewModelScope.launch {
            refreshPendingStripeCheckoutInternal()
        }
    }

    fun clearError() {
        updateTransient(clearErrorMessage = true)
    }

    fun clearPaymentSuccessMessage() {
        updateTransient(clearPaymentSuccessMessage = true)
    }

    fun consumeOpenUrl() {
        updateTransient(clearOpenUrlOnce = true)
    }



    private suspend fun handlePaymentSuccessReturn(
        invoiceId: String,
        sessionId: String?,
        contractorUserId: String? = null
    ) {
        updateTransient(isGeneratingCheckoutSession = false, clearErrorMessage = true)
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

    fun rememberPendingStripeCheckout(request: InvoicePaymentRequest) {
        if (!request.provider.usesStripeRail) return
        val checkoutUrl = request.paymentLink.value
        if (!checkoutUrl.contains("checkout.stripe.com")) return
        pendingCheckoutSessionId = StripeCheckoutUrlParser.extractSessionId(checkoutUrl)
        pendingCheckoutInvoiceId = request.invoiceId.value
        viewModelScope.launch {
            pendingCheckoutContractorId = authRepository.currentUser.first()?.id?.value
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

    suspend fun applyStripePaymentSuccess(invoiceId: String, showToast: Boolean = true) {
        if (invoiceId.isBlank()) return
        val existingInvoice = invoiceRepository.getInvoiceById(InvoiceId(invoiceId))
        val wasAlreadyPaid = existingInvoice?.isPaid == true && existingInvoice.isEstimate == false
        val paidAt = Clock.System.now().toEpochMilliseconds()
        paymentRepository.markInvoicePaid(InvoiceId(invoiceId), paidAt)
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
            clearErrorMessage = true
        )
        clearPendingCheckout()
    }

    fun updateTransient(
        latestRequest: InvoicePaymentRequest? = null,
        errorMessage: String? = null,
        openUrlOnce: String? = null,
        isCreatingPaymentRequest: Boolean? = null,
        lastReceiptUrl: String? = null,
        lastPaidInvoiceId: String? = null,
        isGeneratingCheckoutSession: Boolean? = null,
        paymentSuccessMessage: String? = null,
        activeCheckoutUrl: String? = null,
        checkoutLinkCanPay: Boolean? = null,
        checkoutLinkMessage: String? = null,
        isResolvingCheckoutLink: Boolean? = null,
        clearLatestRequest: Boolean = false,
        clearActiveCheckoutUrl: Boolean = false,
        clearPaymentSuccessMessage: Boolean = false,
        clearErrorMessage: Boolean = false,
        clearOpenUrlOnce: Boolean = false,
        clearCheckoutLinkMessage: Boolean = false
    ) {
        transientState.update { current ->
            current.copy(
                latestRequest = if (clearLatestRequest) null else (latestRequest ?: current.latestRequest),
                errorMessage = if (clearErrorMessage) null else (errorMessage ?: current.errorMessage),
                openUrlOnce = if (clearOpenUrlOnce) null else (openUrlOnce ?: current.openUrlOnce),
                isCreatingPaymentRequest = isCreatingPaymentRequest ?: current.isCreatingPaymentRequest,
                lastReceiptUrl = lastReceiptUrl ?: current.lastReceiptUrl,
                lastPaidInvoiceId = lastPaidInvoiceId ?: current.lastPaidInvoiceId,
                isGeneratingCheckoutSession = isGeneratingCheckoutSession ?: current.isGeneratingCheckoutSession,
                paymentSuccessMessage = if (clearPaymentSuccessMessage) null else (paymentSuccessMessage ?: current.paymentSuccessMessage),
                activeCheckoutUrl = if (clearActiveCheckoutUrl) null else (activeCheckoutUrl ?: current.activeCheckoutUrl),
                checkoutLinkCanPay = checkoutLinkCanPay ?: current.checkoutLinkCanPay,
                checkoutLinkMessage = if (clearCheckoutLinkMessage) null else (checkoutLinkMessage ?: current.checkoutLinkMessage),
                isResolvingCheckoutLink = isResolvingCheckoutLink ?: current.isResolvingCheckoutLink
            )
        }
    }

    private companion object {
        const val POLL_ACTIVE_MS = 5_000L
        const val POLL_IDLE_MS = 15_000L
    }
}
