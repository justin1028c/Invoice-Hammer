package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.ForemanGeminiConfig
import com.fordham.toolbelt.shared.BuildConfig

actual fun createDefaultForemanGeminiConfig(): ForemanGeminiConfig {
    return ForemanGeminiConfig(
        backendBaseUrl = BuildConfig.FOREMAN_GEMINI_BACKEND_URL,
        backendApiKey = BuildConfig.FOREMAN_BACKEND_API_KEY,
        agentModelName = BuildConfig.GEMINI_AGENT_MODEL_NAME,
        taskModelName = BuildConfig.GEMINI_TASK_MODEL_NAME
    )
}
