package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.ForemanGeminiConfig
import com.fordham.toolbelt.util.SecretProvider

fun createDefaultForemanGeminiConfig(secretProvider: SecretProvider): ForemanGeminiConfig {
    val backendBaseUrl = secretProvider.getSecret("foreman_gemini_backend_url")
    val backendApiKey = secretProvider.getSecret("foreman_backend_api_key")
    val agentModelName = secretProvider.getSecret("gemini_agent_model_name")
        .takeIf { it.isNotBlank() }
        ?: secretProvider.getSecret("gemini_model_name")
    val taskModelName = secretProvider.getSecret("gemini_task_model_name")
        .takeIf { it.isNotBlank() }
        ?: agentModelName

    return ForemanGeminiConfig(
        backendBaseUrl = backendBaseUrl,
        backendApiKey = backendApiKey,
        agentModelName = agentModelName,
        taskModelName = taskModelName
    )
}
