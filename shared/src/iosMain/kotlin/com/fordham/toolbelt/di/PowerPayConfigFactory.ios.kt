package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.PowerPayApiVariant
import com.fordham.toolbelt.data.remote.PowerPayConfig
import com.fordham.toolbelt.data.remote.PowerPayEnvironment
import com.fordham.toolbelt.util.IosSecurityServiceProvider

/**
 * iOS reads PowerPay secrets from Keychain via [IosSecurityServiceProvider].
 *
 * Register these keys from Swift before `MainViewControllerKt.initKoinIos()`:
 *  - "powerpay_base_url"      → e.g. https://your-powerpay.vercel.app
 *  - "powerpay_app_id"        → from PowerPay dashboard
 *  - "powerpay_public_key"    → pk_test_... or pk_live_...
 *  - "powerpay_signing_secret"→ whsec_test_... or whsec_live_...
 *  - "powerpay_env"           → "sandbox" or "production"
 *  - "powerpay_api_variant"   → "sdk" or "relay"
 *
 * When any required key is missing, the app falls back to the in-memory mock
 * client (mirrors Android's behaviour when [local.properties] is empty).
 */
actual fun createDefaultPowerPayConfig(): PowerPayConfig {
    val bridge = IosSecurityServiceProvider.bridge ?: return PowerPayConfig(baseUrl = "")
    val baseUrl = bridge.getSecret("powerpay_base_url").orEmpty()
    val appId = bridge.getSecret("powerpay_app_id").orEmpty()

    val envRaw = bridge.getSecret("powerpay_env")?.lowercase().orEmpty()
    val environment = when (envRaw) {
        "production", "prod", "live" -> PowerPayEnvironment.Production
        else -> PowerPayEnvironment.Sandbox
    }

    val apiVariantRaw = bridge.getSecret("powerpay_api_variant")?.lowercase().orEmpty()
    val apiVariant = when (apiVariantRaw) {
        "relay", "relayv1", "james" -> PowerPayApiVariant.RelayV1
        else -> PowerPayApiVariant.SdkV1
    }

    return PowerPayConfig(
        baseUrl = baseUrl,
        appId = appId,
        publicApiKey = bridge.getSecret("powerpay_public_key")?.takeIf { it.isNotBlank() },
        signingSecret = bridge.getSecret("powerpay_signing_secret")?.takeIf { it.isNotBlank() },
        environment = environment,
        preset = PowerPayConfig.PRESET_DIGITAL_TOOL_BELT,
        apiVariant = apiVariant
    )
}
