package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.StripeConfig
import com.fordham.toolbelt.util.IosSecurityServiceProvider

actual fun createDefaultStripeConfig(): StripeConfig {
    val bridge = IosSecurityServiceProvider.bridge
    val publishableKey = bridge?.getSecret("stripe_publishable_key") ?: ""
    val backendUrl = bridge?.getSecret("stripe_payment_backend_url") ?: ""
    val connectUrl = bridge?.getSecret("stripe_connect_onboarding_url") ?: ""
    val backendApiKey = bridge?.getSecret("stripe_backend_api_key") ?: ""
    val feeBps = bridge?.getSecret("stripe_application_fee_bps")?.toIntOrNull()
        ?: StripeConfig.DEFAULT_APPLICATION_FEE_BPS
    return StripeConfig(
        publishableKey = publishableKey,
        paymentBackendBaseUrl = backendUrl,
        connectOnboardingUrl = connectUrl,
        backendApiKey = backendApiKey,
        applicationFeeBps = feeBps
    )
}
