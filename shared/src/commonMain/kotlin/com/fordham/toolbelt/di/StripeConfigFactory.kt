package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.StripeConfig
import com.fordham.toolbelt.util.SecretProvider

fun createDefaultStripeConfig(secretProvider: SecretProvider): StripeConfig {
    val publishableKey = secretProvider.getSecret("stripe_publishable_key")
    val paymentBackendBaseUrl = secretProvider.getSecret("stripe_payment_backend_url")
    val connectOnboardingUrl = secretProvider.getSecret("stripe_connect_onboarding_url")
    val backendApiKey = secretProvider.getSecret("stripe_backend_api_key")
    val applicationFeeBpsString = secretProvider.getSecret("stripe_application_fee_bps")

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
