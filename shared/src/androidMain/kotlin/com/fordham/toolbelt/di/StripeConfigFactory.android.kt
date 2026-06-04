package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.StripeConfig
import com.fordham.toolbelt.shared.BuildConfig

actual fun createDefaultStripeConfig(): StripeConfig {
    val securityManager = runCatching {
        org.koin.core.context.GlobalContext.get().get<com.fordham.toolbelt.util.SecurityManager>()
    }.getOrNull()
    val prefs = securityManager?.getEncryptedPrefs()

    val publishableKey = prefs?.getString("stripe_publishable_key", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.STRIPE_PUBLISHABLE_KEY

    val paymentBackendBaseUrl = prefs?.getString("stripe_payment_backend_url", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.STRIPE_PAYMENT_BACKEND_URL

    val connectOnboardingUrl = prefs?.getString("stripe_connect_onboarding_url", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.STRIPE_CONNECT_ONBOARDING_URL

    val backendApiKey = prefs?.getString("stripe_backend_api_key", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.STRIPE_BACKEND_API_KEY

    val applicationFeeBpsString = prefs?.getString("stripe_application_fee_bps", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.STRIPE_APPLICATION_FEE_BPS

    val applicationFeeBps = applicationFeeBpsString.toIntOrNull()
        ?: StripeConfig.DEFAULT_APPLICATION_FEE_BPS

    return StripeConfig(
        publishableKey = publishableKey,
        paymentBackendBaseUrl = paymentBackendBaseUrl,
        connectOnboardingUrl = connectOnboardingUrl,
        backendApiKey = backendApiKey,
        applicationFeeBps = applicationFeeBps
    )
}
