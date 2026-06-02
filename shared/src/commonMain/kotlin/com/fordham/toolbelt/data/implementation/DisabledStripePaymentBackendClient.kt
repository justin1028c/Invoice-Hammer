package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.StripeConnectOnboardOutcome
import com.fordham.toolbelt.data.remote.StripeConnectStatusOutcome
import com.fordham.toolbelt.data.remote.StripeCreatePaymentIntentRequest
import com.fordham.toolbelt.data.remote.StripePaymentBackendClient
import com.fordham.toolbelt.data.remote.StripePaymentIntentOutcome
import com.fordham.toolbelt.domain.model.stripe.StripeCheckoutSessionOutcome

class DisabledStripePaymentBackendClient : StripePaymentBackendClient {
    override suspend fun createPaymentIntent(
        request: StripeCreatePaymentIntentRequest
    ): StripePaymentIntentOutcome = StripePaymentIntentOutcome.NotConfigured

    override suspend fun fetchConnectStatus(contractorUserId: String): StripeConnectStatusOutcome =
        StripeConnectStatusOutcome.NotConfigured

    override suspend fun startConnectOnboarding(contractorUserId: String): StripeConnectOnboardOutcome =
        StripeConnectOnboardOutcome.NotConfigured

    override suspend fun createCheckoutSession(
        invoiceId: String,
        amountInCents: Long,
        contractorUserId: String
    ): StripeCheckoutSessionOutcome = StripeCheckoutSessionOutcome.NotConfigured
}
