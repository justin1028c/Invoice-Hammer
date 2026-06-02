package com.fordham.toolbelt.data.remote

/**
 * Foreman / Gemini proxy settings (Phase A).
 * The Google API key lives only on Supabase Edge Function secrets.
 */
data class ForemanGeminiConfig(
    val backendBaseUrl: String,
    val backendApiKey: String = "",
    /** High-accuracy model used for Foreman agentic tool-calling (QUICK_INVOICE etc.) */
    val agentModelName: String,
    /** Cheap model used for fire-and-forget tasks: processTask, OCR text extraction */
    val taskModelName: String
) {
    val isBackendConfigured: Boolean get() = backendBaseUrl.isNotBlank()
    val isBackendApiKeyConfigured: Boolean get() = backendApiKey.isNotBlank()
    val isReady: Boolean get() = isBackendConfigured && agentModelName.isNotBlank()

    // Legacy alias so existing call sites compile without changes
    val modelName: String get() = agentModelName
}
