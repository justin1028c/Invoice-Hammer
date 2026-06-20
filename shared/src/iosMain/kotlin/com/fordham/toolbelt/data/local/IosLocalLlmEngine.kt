package com.fordham.toolbelt.data.local

import com.fordham.toolbelt.domain.model.GeminiOutcome
import com.fordham.toolbelt.domain.model.LlmPrompt
import com.fordham.toolbelt.domain.model.FailureMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher

class IosLocalLlmEngine(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
) : LocalLlmEngine {

    override suspend fun isSupported(): Boolean {
        // iOS models are download-on-demand and require hardware support (A17 Pro / M-series / Apple Intelligence).
        // Since we are downloading on first-use, local model inference returns false until the model is ready.
        return false
    }

    override suspend fun generateText(prompt: LlmPrompt): GeminiOutcome {
        // Native Swift bridge delegate stub for CoreML / Apple Intelligence.
        // Once the model download / on-demand orchestration is integrated in the Swift layer, 
        // this delegate will perform native CoreML/Apple Intelligence inference.
        return GeminiOutcome.Failure(FailureMessage("Local LLM is not supported or not downloaded yet on iOS."))
    }
}
