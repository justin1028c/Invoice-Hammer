package com.fordham.toolbelt.data.local

import com.fordham.toolbelt.domain.model.GeminiOutcome
import com.fordham.toolbelt.domain.model.LlmPrompt
import com.fordham.toolbelt.domain.model.FailureMessage
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.common.FeatureStatus
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

class AndroidLocalLlmEngine(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
) : LocalLlmEngine {

    private val client by lazy { Generation.getClient() }

    override suspend fun isSupported(): Boolean {
        return try {
            val status = client.checkStatus()
            com.fordham.toolbelt.util.AppLogger.d("AndroidLocalLlmEngine", "isSupported checkStatus: $status")
            if (status == FeatureStatus.DOWNLOADABLE) {
                scope.launch(ioDispatcher) {
                    try {
                        client.download().catch {
                            com.fordham.toolbelt.util.AppLogger.e("AndroidLocalLlmEngine", "download flow error", it)
                        }.collect {}
                    } catch (e: Exception) {
                        com.fordham.toolbelt.util.AppLogger.e("AndroidLocalLlmEngine", "download catch error", e)
                    }
                }
            }
            status == FeatureStatus.AVAILABLE
        } catch (e: Exception) {
            com.fordham.toolbelt.util.AppLogger.e("AndroidLocalLlmEngine", "isSupported failed with exception", e)
            false
        }
    }

    override suspend fun generateText(prompt: LlmPrompt): GeminiOutcome {
        return try {
            val status = client.checkStatus()
            if (status == FeatureStatus.AVAILABLE) {
                val response = client.generateContent(prompt.value)
                val text = response.candidates.firstOrNull()?.text ?: ""
                GeminiOutcome.Success(text)
            } else {
                if (status == FeatureStatus.DOWNLOADABLE) {
                    scope.launch(ioDispatcher) {
                        try {
                            client.download().catch {}.collect {}
                        } catch (ignored: Exception) {}
                    }
                }
                GeminiOutcome.Failure(FailureMessage("Local LLM is not ready. Status: $status"))
            }
        } catch (e: Exception) {
            GeminiOutcome.Failure(FailureMessage(e.message ?: "Local LLM inference failed"))
        }
    }
}
