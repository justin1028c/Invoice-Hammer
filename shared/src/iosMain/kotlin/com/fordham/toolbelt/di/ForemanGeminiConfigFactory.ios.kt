package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.ForemanGeminiConfig
import com.fordham.toolbelt.util.IosSecurityServiceProvider

actual fun createDefaultForemanGeminiConfig(): ForemanGeminiConfig {
    val bridge = IosSecurityServiceProvider.bridge
    return ForemanGeminiConfig(
        backendBaseUrl = bridge?.getSecret("foreman_gemini_backend_url") ?: "",
        backendApiKey = bridge?.getSecret("foreman_backend_api_key") ?: "",
        agentModelName = bridge?.getSecret("gemini_agent_model_name")?.takeIf { it.isNotBlank() } ?: "gemini-3.5-flash",
        taskModelName = bridge?.getSecret("gemini_task_model_name")?.takeIf { it.isNotBlank() } ?: "gemini-3.1-flash-lite"
    )
}
