package com.fordham.toolbelt.data.remote

/**
 * Stellar PowerPay connection settings. Use preset [dtb] for contractor terminology in hosted UI.
 */
enum class PowerPayApiVariant {
    /** Website/SDK guide: POST /v1/invoice-payments, GET /v1/payments/{id}, GET /v1/events */
    SdkV1,
    /** James relay (PowerPayDtos.kt): POST /api/v1/payments, GET /api/v1/payments/{id} */
    RelayV1
}

data class PowerPayConfig(
    val baseUrl: String,
    val appId: String = "",
    val publicApiKey: String? = null,
    val signingSecret: String? = null,
    val environment: PowerPayEnvironment = PowerPayEnvironment.Sandbox,
    val preset: String = PRESET_DIGITAL_TOOL_BELT,
    val apiVariant: PowerPayApiVariant = PowerPayApiVariant.SdkV1
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && appId.isNotBlank()
    val isTestnet: Boolean get() = environment != PowerPayEnvironment.Production

    companion object {
        const val PRESET_DIGITAL_TOOL_BELT = "dtb"
    }
}

enum class PowerPayEnvironment(val wireName: String) {
    Sandbox("sandbox"),
    Production("production")
}
