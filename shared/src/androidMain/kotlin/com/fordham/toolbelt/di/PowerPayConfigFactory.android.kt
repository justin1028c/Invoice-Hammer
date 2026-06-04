package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.PowerPayApiVariant
import com.fordham.toolbelt.data.remote.PowerPayConfig
import com.fordham.toolbelt.data.remote.PowerPayEnvironment
import com.fordham.toolbelt.shared.BuildConfig

actual fun createDefaultPowerPayConfig(): PowerPayConfig {
    val securityManager = runCatching {
        org.koin.core.context.GlobalContext.get().get<com.fordham.toolbelt.util.SecurityManager>()
    }.getOrNull()
    val prefs = securityManager?.getEncryptedPrefs()

    val baseUrl = prefs?.getString("powerpay_base_url", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.POWERPAY_BASE_URL

    val appId = prefs?.getString("powerpay_app_id", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.POWERPAY_APP_ID

    val publicApiKey = prefs?.getString("powerpay_public_key", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.POWERPAY_PUBLIC_KEY.takeIf { it.isNotBlank() }

    val signingSecret = prefs?.getString("powerpay_signing_secret", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.POWERPAY_SIGNING_SECRET.takeIf { it.isNotBlank() }

    val envString = prefs?.getString("powerpay_env", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.POWERPAY_ENV

    val variantString = prefs?.getString("powerpay_api_variant", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.POWERPAY_API_VARIANT

    val environment = when (envString.lowercase()) {
        "production", "prod", "live" -> PowerPayEnvironment.Production
        else -> PowerPayEnvironment.Sandbox
    }
    val apiVariant = when (variantString.lowercase()) {
        "relay", "relayv1", "james" -> PowerPayApiVariant.RelayV1
        else -> PowerPayApiVariant.SdkV1
    }

    return PowerPayConfig(
        baseUrl = baseUrl,
        appId = appId,
        publicApiKey = publicApiKey,
        signingSecret = signingSecret,
        environment = environment,
        preset = PowerPayConfig.PRESET_DIGITAL_TOOL_BELT,
        apiVariant = apiVariant
    )
}
