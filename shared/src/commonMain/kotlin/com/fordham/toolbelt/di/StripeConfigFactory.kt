package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.StripeConfig
import com.fordham.toolbelt.util.SecretProvider

fun createDefaultStripeConfig(secretProvider: SecretProvider): StripeConfig {
    val publishableKey = secretProvider.getSecret("stripe_publishable_key")
    val paymentBackendBaseUrl = secretProvider.getSecret("stripe_payment_backend_url")
    val connectOnboardingUrl = secretProvider.getSecret("stripe_connect_onboarding_url")
    return StripeConfig(
        publishableKey = publishableKey,
        paymentBackendBaseUrl = paymentBackendBaseUrl,
        connectOnboardingUrl = connectOnboardingUrl
    )
}
