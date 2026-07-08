package com.fordham.toolbelt.data.remote

/**
 * Stripe Connect + Payment Sheet settings.
 * PaymentIntent client secrets must come from your backend (never ship secret keys in the app).
 */
data class StripeConfig(
    val publishableKey: String,
    val paymentBackendBaseUrl: String,
    val connectOnboardingUrl: String = ""
) {
    val isPublishableKeyConfigured: Boolean get() = publishableKey.startsWith("pk_")
    val isBackendConfigured: Boolean get() = paymentBackendBaseUrl.isNotBlank()
    val isPaymentSheetReady: Boolean get() = isPublishableKeyConfigured && isBackendConfigured
}
