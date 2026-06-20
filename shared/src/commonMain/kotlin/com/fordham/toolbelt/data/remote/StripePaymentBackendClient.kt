package com.fordham.toolbelt.data.remote

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.stripe.StripeCheckoutSessionOutcome

interface StripePaymentBackendClient {
    suspend fun createPaymentIntent(request: StripeCreatePaymentIntentRequest): StripePaymentIntentOutcome
    suspend fun fetchConnectStatus(contractorUserId: String): StripeConnectStatusOutcome
    suspend fun startConnectOnboarding(contractorUserId: String): StripeConnectOnboardOutcome
    suspend fun createCheckoutSession(invoiceId: String, amountInCents: Long, contractorUserId: String): StripeCheckoutSessionOutcome
    suspend fun verifyCheckoutSession(
        sessionId: String,
        contractorUserId: String? = null
    ): StripeCheckoutVerifyOutcome
    suspend fun fetchInvoicePaymentStatus(invoiceId: String, contractorUserId: String): StripeInvoicePaymentStatusOutcome
    suspend fun resolveCheckoutLink(sessionId: String, contractorUserId: String): StripeCheckoutLinkOutcome
}

sealed interface StripePaymentIntentOutcome {
    data class Success(val response: StripeCreatePaymentIntentResponse) : StripePaymentIntentOutcome
    data object NotConfigured : StripePaymentIntentOutcome
    data class Failure(
        val error: FailureMessage,
        val actionUrl: String? = null
    ) : StripePaymentIntentOutcome
}

sealed interface StripeConnectStatusOutcome {
    data class Success(val status: StripeConnectAccountStatusResponse) : StripeConnectStatusOutcome
    data object NotConfigured : StripeConnectStatusOutcome
    data class Failure(val error: FailureMessage) : StripeConnectStatusOutcome
}

sealed interface StripeConnectOnboardOutcome {
    data class Success(val response: StripeConnectOnboardResponse) : StripeConnectOnboardOutcome
    data object NotConfigured : StripeConnectOnboardOutcome
    data class Failure(val error: FailureMessage) : StripeConnectOnboardOutcome
}

sealed interface StripeCheckoutVerifyOutcome {
    data class Success(val response: StripeCheckoutVerifyResponse) : StripeCheckoutVerifyOutcome
    data object NotConfigured : StripeCheckoutVerifyOutcome
    data class Failure(val error: FailureMessage) : StripeCheckoutVerifyOutcome
}

sealed interface StripeInvoicePaymentStatusOutcome {
    data class Success(val response: StripeInvoicePaymentStatusResponse) : StripeInvoicePaymentStatusOutcome
    data object NotConfigured : StripeInvoicePaymentStatusOutcome
    data class Failure(val error: FailureMessage) : StripeInvoicePaymentStatusOutcome
}

sealed interface StripeCheckoutLinkOutcome {
    data class Success(val response: StripeCheckoutLinkResponse) : StripeCheckoutLinkOutcome
    data object NotConfigured : StripeCheckoutLinkOutcome
    data class Failure(val error: FailureMessage) : StripeCheckoutLinkOutcome
}
