package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestOutcome
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.usesStripeRail
import com.fordham.toolbelt.domain.model.stripe.StripePaymentMode
import com.fordham.toolbelt.domain.model.stripe.StripeCheckoutSessionOutcome
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.StripeCheckoutRepository
import com.fordham.toolbelt.domain.usecase.CreatePaymentRequestUseCase
import com.fordham.toolbelt.domain.usecase.stripe.GetStripePaymentModeUseCase
import com.fordham.toolbelt.domain.usecase.stripe.ResolveStripeCheckoutLinkUseCase
import com.fordham.toolbelt.domain.usecase.stripe.ResolveStripeCheckoutLinkOutcome
import com.fordham.toolbelt.domain.payment.stripe.StripeCheckoutUrlParser
import com.fordham.toolbelt.util.AppLogger
import com.fordham.toolbelt.util.PlatformActions
import com.fordham.toolbelt.util.UiMessageKeys
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StripeConnectViewModel(
    private val invoicePaymentViewModel: InvoicePaymentViewModel,
    private val createPaymentRequestUseCase: CreatePaymentRequestUseCase,
    getStripePaymentModeUseCase: GetStripePaymentModeUseCase,
    private val stripeCheckoutRepository: StripeCheckoutRepository,
    private val resolveStripeCheckoutLinkUseCase: ResolveStripeCheckoutLinkUseCase,
    private val platformActions: PlatformActions,
    private val authRepository: AuthRepository,
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    val stripePaymentMode: StripePaymentMode = getStripePaymentModeUseCase()

    fun requestDeposit(invoice: Invoice, provider: PaymentProviderType) {
        createRequest(invoice, PaymentRequestType.Deposit, provider)
    }

    fun requestFullPayment(invoice: Invoice, provider: PaymentProviderType) {
        createRequest(invoice, PaymentRequestType.FullBalance, provider)
    }

    fun regenerateCheckoutLink() {
        val request = invoicePaymentViewModel.uiState.value.latestRequest ?: return
        viewModelScope.launch {
            val invoice = invoiceRepository.getInvoiceById(request.invoiceId) ?: return@launch
            createRequest(invoice, request.type, request.provider)
        }
    }

    fun refreshActiveCheckoutLink() {
        invoicePaymentViewModel.uiState.value.latestRequest?.let { resolveActiveCheckoutLink(it) }
    }

    fun createRequest(
        invoice: Invoice,
        type: PaymentRequestType,
        provider: PaymentProviderType,
        onFinished: () -> Unit = {}
    ) {
        viewModelScope.launch {
            AppLogger.d("PaymentFlow", "createRequest: invoice=${invoice.id.value} total=${invoice.totalAmount.value} type=$type provider=$provider")
            invoicePaymentViewModel.updateTransient(
                isCreatingPaymentRequest = true,
                clearErrorMessage = true,
                clearOpenUrlOnce = true
            )
            try {
                val outcome = createPaymentRequestUseCase(invoice, type, provider)
                AppLogger.d("PaymentFlow", "createRequest outcome=$outcome")
                when (outcome) {
                    is PaymentRequestOutcome.Success -> {
                        invoicePaymentViewModel.rememberPendingStripeCheckout(outcome.request)
                        invoicePaymentViewModel.updateTransient(
                            latestRequest = outcome.request,
                            clearActiveCheckoutUrl = true,
                            checkoutLinkCanPay = true,
                            clearErrorMessage = true,
                            clearOpenUrlOnce = true,
                            isCreatingPaymentRequest = false
                        )
                        resolveActiveCheckoutLink(outcome.request)
                    }
                    is PaymentRequestOutcome.Failure -> {
                        AppLogger.d("PaymentFlow", "createRequest Failure error=${outcome.error.value}")
                        invoicePaymentViewModel.updateTransient(
                            errorMessage = outcome.error.value,
                            openUrlOnce = outcome.actionUrl,
                            isCreatingPaymentRequest = false
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("PaymentFlow", "createRequest crashed", e)
                invoicePaymentViewModel.updateTransient(
                    errorMessage = e.message ?: "Unknown error occurred during payment request creation",
                    isCreatingPaymentRequest = false
                )
            } finally {
                AppLogger.d("PaymentFlow", "createRequest finally: calling onFinished")
                onFinished()
            }
        }
    }

    fun resolveActiveCheckoutLink(request: InvoicePaymentRequest) {
        AppLogger.d("PaymentFlow", "resolveActiveCheckoutLink: id=${request.id.value} provider=${request.provider} link=${request.paymentLink.value}")
        if (!request.provider.usesStripeRail ||
            !StripeCheckoutUrlParser.isHostedCheckoutUrl(request.paymentLink.value)
        ) {
            AppLogger.d("PaymentFlow", "resolveActiveCheckoutLink: fallback/demo URL")
            invoicePaymentViewModel.updateTransient(
                activeCheckoutUrl = request.paymentLink.value,
                checkoutLinkCanPay = true,
                clearCheckoutLinkMessage = true,
                isResolvingCheckoutLink = false
            )
            return
        }

        val sessionId = StripeCheckoutUrlParser.extractSessionId(request.paymentLink.value)
        AppLogger.d("PaymentFlow", "resolveActiveCheckoutLink: extracted sessionId=$sessionId")
        if (sessionId.isNullOrBlank()) {
            invoicePaymentViewModel.updateTransient(
                activeCheckoutUrl = request.paymentLink.value,
                checkoutLinkCanPay = true,
                clearCheckoutLinkMessage = true,
                isResolvingCheckoutLink = false
            )
            return
        }

        viewModelScope.launch {
            AppLogger.d("PaymentFlow", "resolveActiveCheckoutLink: launching resolve coroutine")
            invoicePaymentViewModel.updateTransient(
                isResolvingCheckoutLink = true,
                clearCheckoutLinkMessage = true
            )
            try {
                when (val outcome = resolveStripeCheckoutLinkUseCase(sessionId)) {
                    is ResolveStripeCheckoutLinkOutcome.Resolved -> {
                        AppLogger.d("PaymentFlow", "resolveStripeCheckoutLink Resolved: canPay=${outcome.canPay} paid=${outcome.paid} url=${outcome.checkoutUrl}")
                        when {
                            outcome.canPay -> {
                                val resolvedUrl = outcome.checkoutUrl ?: request.paymentLink.value
                                invoicePaymentViewModel.updateTransient(
                                    activeCheckoutUrl = resolvedUrl,
                                    checkoutLinkCanPay = true,
                                    clearCheckoutLinkMessage = true,
                                    isResolvingCheckoutLink = false
                                )
                            }
                            outcome.paid -> {
                                invoicePaymentViewModel.updateTransient(
                                    clearActiveCheckoutUrl = true,
                                    checkoutLinkCanPay = false,
                                    checkoutLinkMessage = UiMessageKeys.CHECKOUT_LINK_ALREADY_PAID,
                                    isResolvingCheckoutLink = false
                                )
                            }
                            else -> {
                                invoicePaymentViewModel.updateTransient(
                                    clearActiveCheckoutUrl = true,
                                    checkoutLinkCanPay = false,
                                    checkoutLinkMessage = UiMessageKeys.CHECKOUT_LINK_EXPIRED,
                                    isResolvingCheckoutLink = false
                                )
                            }
                        }
                    }
                    is ResolveStripeCheckoutLinkOutcome.Failure -> {
                        AppLogger.d("PaymentFlow", "resolveStripeCheckoutLink Failure: ${outcome.error.value}")
                        invoicePaymentViewModel.updateTransient(
                            activeCheckoutUrl = request.paymentLink.value,
                            checkoutLinkCanPay = true,
                            checkoutLinkMessage = outcome.error.value,
                            isResolvingCheckoutLink = false
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("PaymentFlow", "resolveActiveCheckoutLink crashed during API call", e)
                invoicePaymentViewModel.updateTransient(
                    activeCheckoutUrl = request.paymentLink.value,
                    checkoutLinkCanPay = true,
                    checkoutLinkMessage = e.message ?: "Failed to resolve payment link.",
                    isResolvingCheckoutLink = false
                )
            }
        }
    }

    fun generateAndLaunchStripeCheckout(invoice: Invoice, type: PaymentRequestType) {
        viewModelScope.launch {
            invoicePaymentViewModel.updateTransient(
                isGeneratingCheckoutSession = true,
                clearErrorMessage = true,
                clearPaymentSuccessMessage = true
            )
            val amount = when (type) {
                PaymentRequestType.Deposit ->
                    invoice.depositAmount.value.takeIf { it > 0.0 } ?: (invoice.totalAmount.value * 0.30)
                PaymentRequestType.FullBalance -> invoice.totalAmount.value
            }
            val amountInCents = (amount * 100).toLong()
            val contractorId = authRepository.currentUser.first()?.id?.value ?: "anonymous"

            when (val outcome = stripeCheckoutRepository.generateFallbackPaymentLink(invoice.id, amountInCents, contractorId)) {
                StripeCheckoutSessionOutcome.NotConfigured -> {
                    invoicePaymentViewModel.updateTransient(
                        isGeneratingCheckoutSession = false,
                        errorMessage = UiMessageKeys.STRIPE_CONNECT_NOT_CONFIGURED
                    )
                }
                is StripeCheckoutSessionOutcome.Failure -> {
                    invoicePaymentViewModel.updateTransient(
                        isGeneratingCheckoutSession = false,
                        errorMessage = outcome.error.value
                    )
                }
                is StripeCheckoutSessionOutcome.Success -> {
                    invoicePaymentViewModel.updateTransient(isGeneratingCheckoutSession = false)
                    platformActions.openUrl(outcome.checkoutUrl.value)
                }
            }
        }
    }

    fun selectPaymentRequest(request: InvoicePaymentRequest) {
        invoicePaymentViewModel.updateTransient(
            latestRequest = request,
            clearActiveCheckoutUrl = true,
            checkoutLinkCanPay = true,
            clearErrorMessage = true,
            clearOpenUrlOnce = true,
            isCreatingPaymentRequest = false
        )
        resolveActiveCheckoutLink(request)
    }
}
