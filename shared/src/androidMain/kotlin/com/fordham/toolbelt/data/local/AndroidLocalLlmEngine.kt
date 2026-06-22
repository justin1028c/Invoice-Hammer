package com.fordham.toolbelt.data.local

import android.content.Context
import com.fordham.toolbelt.domain.model.GeminiOutcome
import com.fordham.toolbelt.domain.model.LlmPrompt
import com.fordham.toolbelt.domain.model.FailureMessage
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.readBytes
import java.io.File
import java.io.FileOutputStream

class AndroidLocalLlmEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
) : LocalLlmEngine {

    private val modelFile = File(context.filesDir, "llama-3.2-3b.task")
    private var inference: LlmInference? = null
    private var isDownloading = false
    private var downloadProgress = 0.0f
    
    // Remote endpoint hosting the compiled Llama 3.2 3B task file for MediaPipe Tasks GenAI
    private val modelUrl = "https://huggingface.co/google/mediapipe/resolve/main/llama-3.2-3b-instruct-gpu.task"

    override suspend fun isSupported(): Boolean {
        if (inference != null) return true
        if (!isModelDownloaded()) return false
        
        return withContext(ioDispatcher) {
            try {
                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(1024)
                    .build()
                inference = LlmInference.createFromOptions(context, options)
                true
            } catch (e: Exception) {
                com.fordham.toolbelt.util.AppLogger.e("AndroidLocalLlmEngine", "Failed to initialize LlmInference", e)
                false
            }
        }
    }

    override suspend fun generateText(prompt: LlmPrompt): GeminiOutcome {
        if (!isSupported()) {
            return GeminiOutcome.Failure(FailureMessage("Local LLM is not ready or not supported."))
        }
        return withContext(ioDispatcher) {
            try {
                val response = inference?.generateResponse(prompt.value) ?: ""
                GeminiOutcome.Success(response)
            } catch (e: Exception) {
                GeminiOutcome.Failure(FailureMessage(e.message ?: "Local Llama inference failed"))
            }
        }
    }

    override fun isModelDownloaded(): Boolean {
        return modelFile.exists() && modelFile.length() > 100_000_000
    }

    override fun getDownloadProgress(): Float {
        return downloadProgress
    }

    override fun isDownloading(): Boolean {
        return isDownloading
    }

    override fun startDownload(onProgress: (Float) -> Unit, onComplete: (Boolean) -> Unit) {
        if (isDownloading) return
        isDownloading = true
        downloadProgress = 0.0f
        
        scope.launch(ioDispatcher) {
            val client = HttpClient()
            try {
                com.fordham.toolbelt.util.AppLogger.d("AndroidLocalLlmEngine", "Starting model download from $modelUrl")
                val response = client.get(modelUrl) {
                    onDownload { bytesSentTotal, contentLength ->
                        if (contentLength != null && contentLength > 0) {
                            downloadProgress = bytesSentTotal.toFloat() / contentLength
                            onProgress(downloadProgress)
                        }
                    }
                }
                
                val channel: ByteReadChannel = response.bodyAsChannel()
                modelFile.parentFile?.mkdirs()
                val outputStream = FileOutputStream(modelFile)
                val buffer = ByteArray(8192)
                
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer)
                    if (read > 0) {
                        outputStream.write(buffer, 0, read)
                    }
                }
                outputStream.close()
                com.fordham.toolbelt.util.AppLogger.d("AndroidLocalLlmEngine", "Download complete. Model size: ${modelFile.length()}")
                
                isDownloading = false
                downloadProgress = 1.0f
                onComplete(true)
            } catch (e: Exception) {
                com.fordham.toolbelt.util.AppLogger.e("AndroidLocalLlmEngine", "Download failed", e)
                if (modelFile.exists()) modelFile.delete()
                isDownloading = false
                downloadProgress = 0.0f
                onComplete(false)
            } finally {
                client.close()
            }
        }
    }

    override fun deleteModel(): Boolean {
        inference = null
        if (modelFile.exists()) {
            return modelFile.delete()
        }
        return false
    }
}
