package com.fordham.toolbelt.data.local

import com.fordham.toolbelt.domain.model.GeminiOutcome
import com.fordham.toolbelt.domain.model.LlmPrompt

interface LocalLlmEngine {
    suspend fun isSupported(): Boolean
    suspend fun generateText(prompt: LlmPrompt): GeminiOutcome

    fun isModelDownloaded(): Boolean
    fun getDownloadProgress(): Float
    fun isDownloading(): Boolean
    fun startDownload(onProgress: (Float) -> Unit, onComplete: (Boolean) -> Unit)
    fun deleteModel(): Boolean
}
