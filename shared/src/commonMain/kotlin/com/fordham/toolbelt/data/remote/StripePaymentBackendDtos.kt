package com.fordham.toolbelt.data.remote

import kotlinx.serialization.Serializable
import com.fordham.toolbelt.util.randomUUID

@Serializable
data class StripeCreatePaymentIntentRequest(
    val operationId: String = randomUUID(),
    val amountCents: Long,
    val currency: String = "usd",
    val invoiceId: String,
    val contractorUserId: String,
    val clientName: String,
    val requestType: String,
    /** google_pay | apple_pay | card_link — selects Stripe payment method profile on backend */
    val paymentProvider: String
)

@Serializable
data class StripeCreatePaymentIntentResponse(
    val clientSecret: String,
    val paymentIntentId: String,
    /** Connect Express account that owns this PaymentIntent (required for Payment Sheet). */
    val stripeAccountId: String,
    /** Client-facing Stripe Checkout / Payment Link URL when backend creates a session */
    val hostedCheckoutUrl: String? = null,
    val checkoutSessionId: String? = null
)

@Serializable
data class StripeConnectAccountStatusResponse(
    val accountId: String? = null,
    val chargesEnabled: Boolean = false,
    val payoutsEnabled: Boolean = false
)

@Serializable
data class StripeConnectOnboardRequest(
    val contractorUserId: String
)

@Serializable
data class StripeConnectOnboardResponse(
    val onboardingUrl: String,
    val accountId: String? = null
)

@Serializable
data class StripeBackendErrorBody(
    val error: String? = null,
    val onboardingUrl: String? = null
)

@Serializable
data class StripeCreateCheckoutSessionRequest(
    val amountCents: Long,
    val currency: String = "usd",
    val invoiceId: String,
    val contractorUserId: String
)

@Serializable
data class StripeCreateCheckoutSessionResponse(
    val checkoutUrl: String
)

@Serializable
data class StripeCheckoutVerifyResponse(
    val paid: Boolean = false,
    val invoiceId: String? = null
)

@Serializable
data class StripeInvoicePaymentStatusResponse(
    val paid: Boolean = false,
    val paidAt: String? = null
)

@Serializable
data class StripeCheckoutLinkResponse(
    val checkoutUrl: String? = null,
    val status: String = "unknown",
    val paid: Boolean = false,
    val canPay: Boolean = false
)
