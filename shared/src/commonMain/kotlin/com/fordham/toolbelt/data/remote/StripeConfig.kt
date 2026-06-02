package com.fordham.toolbelt.data.remote

/**
 * Stripe Connect + Payment Sheet settings.
 * PaymentIntent client secrets must come from your backend (never ship secret keys in the app).
 */
data class StripeConfig(
    val publishableKey: String,
    val paymentBackendBaseUrl: String,
    val connectOnboardingUrl: String = "",
    val backendApiKey: String = "",
    val applicationFeeBps: Int = DEFAULT_APPLICATION_FEE_BPS
) {
    val isPublishableKeyConfigured: Boolean get() = publishableKey.startsWith("pk_")
    val isBackendConfigured: Boolean get() = paymentBackendBaseUrl.isNotBlank()
    val isBackendApiKeyConfigured: Boolean get() = backendApiKey.isNotBlank()
    val isPaymentSheetReady: Boolean get() = isPublishableKeyConfigured && isBackendConfigured

    companion object {
        /** 1.0% platform fee when using Stripe Connect application_fee_amount */
        const val DEFAULT_APPLICATION_FEE_BPS = 100
    }
}
