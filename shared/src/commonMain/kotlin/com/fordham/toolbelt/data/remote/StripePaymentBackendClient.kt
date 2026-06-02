package com.fordham.toolbelt.data.remote

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.stripe.StripeCheckoutSessionOutcome

interface StripePaymentBackendClient {
    suspend fun createPaymentIntent(request: StripeCreatePaymentIntentRequest): StripePaymentIntentOutcome
    suspend fun fetchConnectStatus(contractorUserId: String): StripeConnectStatusOutcome
    suspend fun startConnectOnboarding(contractorUserId: String): StripeConnectOnboardOutcome
    suspend fun createCheckoutSession(invoiceId: String, amountInCents: Long, contractorUserId: String): StripeCheckoutSessionOutcome
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
