package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.ForemanGeminiConfig
import com.fordham.toolbelt.shared.BuildConfig

actual fun createDefaultForemanGeminiConfig(): ForemanGeminiConfig {
    val securityManager = runCatching {
        org.koin.core.context.GlobalContext.get().get<com.fordham.toolbelt.util.SecurityManager>()
    }.getOrNull()
    val prefs = securityManager?.getEncryptedPrefs()

    val backendBaseUrl = prefs?.getString("foreman_gemini_backend_url", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.FOREMAN_GEMINI_BACKEND_URL

    val backendApiKey = prefs?.getString("foreman_backend_api_key", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.FOREMAN_BACKEND_API_KEY

    val agentModelName = prefs?.getString("gemini_agent_model_name", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.GEMINI_AGENT_MODEL_NAME

    val taskModelName = prefs?.getString("gemini_task_model_name", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.GEMINI_TASK_MODEL_NAME

    return ForemanGeminiConfig(
        backendBaseUrl = backendBaseUrl,
        backendApiKey = backendApiKey,
        agentModelName = agentModelName,
        taskModelName = taskModelName
    )
}
