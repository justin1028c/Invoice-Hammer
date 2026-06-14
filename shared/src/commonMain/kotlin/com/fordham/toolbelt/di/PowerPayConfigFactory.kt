package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.PowerPayApiVariant
import com.fordham.toolbelt.data.remote.PowerPayConfig
import com.fordham.toolbelt.data.remote.PowerPayEnvironment
import com.fordham.toolbelt.util.SecretProvider

fun createDefaultPowerPayConfig(secretProvider: SecretProvider): PowerPayConfig {
    val baseUrl = secretProvider.getSecret("powerpay_base_url")
    val appId = secretProvider.getSecret("powerpay_app_id")
    val publicApiKey = secretProvider.getSecret("powerpay_public_key").takeIf { it.isNotBlank() }
    val signingSecret = secretProvider.getSecret("powerpay_signing_secret").takeIf { it.isNotBlank() }
    val envString = secretProvider.getSecret("powerpay_env").lowercase()
    val variantString = secretProvider.getSecret("powerpay_api_variant").lowercase()

    val environment = when (envString) {
        "production", "prod", "live" -> PowerPayEnvironment.Production
        else -> PowerPayEnvironment.Sandbox
    }
    val apiVariant = when (variantString) {
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
