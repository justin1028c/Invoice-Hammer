package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.StripeConfig
import com.fordham.toolbelt.shared.BuildConfig

actual fun createDefaultStripeConfig(): StripeConfig {
    return StripeConfig(
        publishableKey = BuildConfig.STRIPE_PUBLISHABLE_KEY,
        paymentBackendBaseUrl = BuildConfig.STRIPE_PAYMENT_BACKEND_URL,
        connectOnboardingUrl = BuildConfig.STRIPE_CONNECT_ONBOARDING_URL,
        backendApiKey = BuildConfig.STRIPE_BACKEND_API_KEY,
        applicationFeeBps = BuildConfig.STRIPE_APPLICATION_FEE_BPS.toIntOrNull()
            ?: StripeConfig.DEFAULT_APPLICATION_FEE_BPS
    )
}
