package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.PowerPayApiVariant
import com.fordham.toolbelt.data.remote.PowerPayConfig
import com.fordham.toolbelt.data.remote.PowerPayEnvironment
import com.fordham.toolbelt.shared.BuildConfig

actual fun createDefaultPowerPayConfig(): PowerPayConfig {
    val environment = when (BuildConfig.POWERPAY_ENV.lowercase()) {
        "production", "prod", "live" -> PowerPayEnvironment.Production
        else -> PowerPayEnvironment.Sandbox
    }
    val apiVariant = when (BuildConfig.POWERPAY_API_VARIANT.lowercase()) {
        "relay", "relayv1", "james" -> PowerPayApiVariant.RelayV1
        else -> PowerPayApiVariant.SdkV1
    }
    return PowerPayConfig(
        baseUrl = BuildConfig.POWERPAY_BASE_URL,
        appId = BuildConfig.POWERPAY_APP_ID,
        publicApiKey = BuildConfig.POWERPAY_PUBLIC_KEY.takeIf { it.isNotBlank() },
        signingSecret = BuildConfig.POWERPAY_SIGNING_SECRET.takeIf { it.isNotBlank() },
        environment = environment,
        preset = PowerPayConfig.PRESET_DIGITAL_TOOL_BELT,
        apiVariant = apiVariant
    )
}
