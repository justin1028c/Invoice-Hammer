package com.fordham.toolbelt.data.local

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.fordham.toolbelt.domain.model.GeminiOutcome
import com.fordham.toolbelt.domain.model.LlmPrompt
import com.fordham.toolbelt.domain.model.FailureMessage
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AndroidLocalLlmEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
) : LocalLlmEngine {

    private val modelFile = File(context.filesDir, "gemma-2b-it-cpu-int4.bin")
    private val tempFile = File(context.getExternalFilesDir(null), "gemma-2b-it-cpu-int4.bin.tmp")
    private val sharedPrefs = context.getSharedPreferences("llm_downloader_prefs", Context.MODE_PRIVATE)
    
    private var inference: LlmInference? = null
    private var isDownloading = false
    private var downloadProgress = 0.0f

    private var activeProgressCallback: ((Float) -> Unit)? = null
    private var activeCompleteCallback: ((Boolean) -> Unit)? = null

    // Remote endpoint hosting the compiled Gemma 2B INT4 file for MediaPipe Tasks GenAI
    private val modelUrl = "https://huggingface.co/ASahu16/gemma/resolve/main/gemma-2b-it-cpu-int4.bin"

    init {
        // Safely purge legacy Llama 3.2 3B and 1B models to recover up to ~7.5GB of user device space
        try {
            val legacy3b = File(context.filesDir, "llama-3.2-3b.task")
            if (legacy3b.exists()) legacy3b.delete()
            val legacy3bTmp = File(context.getExternalFilesDir(null), "llama-3.2-3b.task.tmp")
            if (legacy3bTmp.exists()) legacy3bTmp.delete()

            val legacy1b = File(context.filesDir, "llama-3.2-1b.task")
            if (legacy1b.exists()) legacy1b.delete()
            val legacy1bTmp = File(context.getExternalFilesDir(null), "llama-3.2-1b.task.tmp")
            if (legacy1bTmp.exists()) legacy1bTmp.delete()
        } catch (e: Exception) {
            com.fordham.toolbelt.util.AppLogger.e("AndroidLocalLlmEngine", "Failed to clean legacy model files", e)
        }

        val downloadId = sharedPrefs.getLong("download_id", -1L)
        if (downloadId != -1L) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = if (statusCol >= 0) cursor.getInt(statusCol) else -1
                if (status == DownloadManager.STATUS_RUNNING || 
                    status == DownloadManager.STATUS_PENDING || 
                    status == DownloadManager.STATUS_PAUSED) {
                    isDownloading = true
                    startPolling(downloadId)
                } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    isDownloading = false
                    downloadProgress = 1.0f
                    moveTempToInternal()
                    sharedPrefs.edit().remove("download_id").apply()
                } else {
                    isDownloading = false
                    if (tempFile.exists()) tempFile.delete()
                    sharedPrefs.edit().remove("download_id").apply()
                }
            } else {
                sharedPrefs.edit().remove("download_id").apply()
            }
            cursor?.close()
        }
    }

    private fun moveTempToInternal(): Boolean {
        return try {
            if (!tempFile.exists()) return false
            if (modelFile.exists()) modelFile.delete()
            modelFile.parentFile?.mkdirs()
            tempFile.copyTo(modelFile, overwrite = true)
            tempFile.delete()
            true
        } catch (e: Exception) {
            com.fordham.toolbelt.util.AppLogger.e("AndroidLocalLlmEngine", "Failed to move downloaded model file", e)
            false
        }
    }

    override suspend fun isSupported(): Boolean {
        if (inference != null) return true
        if (!isModelDownloaded()) return false
        
        return withContext(ioDispatcher) {
            try {
                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(2048)
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
                GeminiOutcome.Failure(FailureMessage(e.message ?: "Local Gemma inference failed"))
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
        activeProgressCallback = onProgress
        activeCompleteCallback = onComplete

        if (isDownloading) {
            onProgress(downloadProgress)
            return
        }

        if (isModelDownloaded()) {
            onComplete(true)
            return
        }

        isDownloading = true
        downloadProgress = 0.0f

        scope.launch(ioDispatcher) {
            try {
                if (tempFile.exists()) tempFile.delete()
                
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val request = DownloadManager.Request(Uri.parse(modelUrl))
                    .setTitle("Gemma 2B Offline Model")
                    .setDescription("Downloading Gemma 2B model for offline AI capabilities...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverMetered(false)
                    .setAllowedOverRoaming(false)
                    .setDestinationUri(Uri.fromFile(tempFile))

                val downloadId = downloadManager.enqueue(request)
                sharedPrefs.edit().putLong("download_id", downloadId).apply()
                
                startPolling(downloadId)
            } catch (e: Exception) {
                com.fordham.toolbelt.util.AppLogger.e("AndroidLocalLlmEngine", "Failed to enqueue download", e)
                isDownloading = false
                downloadProgress = 0.0f
                if (tempFile.exists()) tempFile.delete()
                sharedPrefs.edit().remove("download_id").apply()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    private fun startPolling(downloadId: Long) {
        scope.launch(ioDispatcher) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            var polling = true
            while (polling) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = if (statusCol >= 0) cursor.getInt(statusCol) else -1

                    val downloadedCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val downloadedBytes = if (downloadedCol >= 0) cursor.getLong(downloadedCol) else 0L

                    val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val totalBytes = if (totalCol >= 0) cursor.getLong(totalCol) else 0L

                    when (status) {
                        DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                            if (totalBytes > 0) {
                                downloadProgress = downloadedBytes.toFloat() / totalBytes
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    activeProgressCallback?.invoke(downloadProgress)
                                }
                            }
                        }
                        DownloadManager.STATUS_PAUSED -> {
                            if (totalBytes > 0) {
                                downloadProgress = downloadedBytes.toFloat() / totalBytes
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    activeProgressCallback?.invoke(downloadProgress)
                                }
                            }
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            polling = false
                            isDownloading = false
                            downloadProgress = 1.0f
                            
                            val success = moveTempToInternal()
                            sharedPrefs.edit().remove("download_id").apply()

                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                activeCompleteCallback?.invoke(success)
                            }
                        }
                        DownloadManager.STATUS_FAILED -> {
                            polling = false
                            isDownloading = false
                            downloadProgress = 0.0f

                            if (tempFile.exists()) tempFile.delete()
                            sharedPrefs.edit().remove("download_id").apply()

                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                activeCompleteCallback?.invoke(false)
                            }
                        }
                    }
                } else {
                    polling = false
                    isDownloading = false
                    downloadProgress = 0.0f
                    sharedPrefs.edit().remove("download_id").apply()
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        activeCompleteCallback?.invoke(false)
                    }
                }
                cursor?.close()
                if (polling) {
                    kotlinx.coroutines.delay(500)
                }
            }
        }
    }

    override fun deleteModel(): Boolean {
        try {
            inference?.close()
        } catch (e: Exception) {
            com.fordham.toolbelt.util.AppLogger.e("AndroidLocalLlmEngine", "Failed to close LlmInference instance", e)
        }
        inference = null
        val downloadId = sharedPrefs.getLong("download_id", -1L)
        if (downloadId != -1L) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(downloadId)
            sharedPrefs.edit().remove("download_id").apply()
        }
        isDownloading = false
        downloadProgress = 0.0f
        if (tempFile.exists()) {
            tempFile.delete()
        }
        if (modelFile.exists()) {
            return modelFile.delete()
        }
        return false
    }
}
