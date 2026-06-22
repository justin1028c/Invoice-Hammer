package com.fordham.toolbelt.data.local

import com.fordham.toolbelt.domain.model.GeminiOutcome
import com.fordham.toolbelt.domain.model.LlmPrompt
import com.fordham.toolbelt.domain.model.FailureMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface IosLocalLlmBridge {
    fun isModelDownloaded(): Boolean
    fun getDownloadProgress(): Float
    fun isDownloading(): Boolean
    fun startDownload(onProgress: (Double) -> Unit, onComplete: (Boolean) -> Unit)
    fun deleteModel(): Boolean
    fun isSupported(): Boolean
    fun generateText(prompt: String, onResult: (String?) -> Unit)
}

class IosLocalLlmEngine(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
) : LocalLlmEngine {

    companion object {
        var bridge: IosLocalLlmBridge? = null
    }

    override suspend fun isSupported(): Boolean {
        return bridge?.isSupported() ?: false
    }

    override suspend fun generateText(prompt: LlmPrompt): GeminiOutcome {
        val currentBridge = bridge ?: return GeminiOutcome.Failure(FailureMessage("Local LLM is not initialized on iOS."))
        
        return withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                currentBridge.generateText(prompt.value) { result ->
                    if (result != null) {
                        continuation.resume(GeminiOutcome.Success(result))
                    } else {
                        continuation.resume(GeminiOutcome.Failure(FailureMessage("Local LLM generation failed on iOS.")))
                    }
                }
            }
        }
    }

    override fun isModelDownloaded(): Boolean {
        return bridge?.isModelDownloaded() ?: false
    }

    override fun getDownloadProgress(): Float {
        return bridge?.getDownloadProgress() ?: 0.0f
    }

    override fun isDownloading(): Boolean {
        return bridge?.isDownloading() ?: false
    }

    override fun startDownload(onProgress: (Float) -> Unit, onComplete: (Boolean) -> Unit) {
        val currentBridge = bridge ?: return onComplete(false)
        currentBridge.startDownload(
            onProgress = { progress -> onProgress(progress.toFloat()) },
            onComplete = onComplete
        )
    }

    override fun deleteModel(): Boolean {
        return bridge?.deleteModel() ?: false
    }
}
