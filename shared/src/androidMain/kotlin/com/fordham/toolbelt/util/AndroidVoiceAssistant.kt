package com.fordham.toolbelt.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.media.MediaRecorder
import java.io.File
import com.fordham.toolbelt.domain.repository.GeminiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AndroidVoiceAssistant(
    private val context: Context,
    private val geminiRepository: GeminiRepository
) : TextToSpeech.OnInitListener, VoiceAssistant {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var onResultCallback: ((VoiceTranscriptMeta) -> Unit)? = null
    private var onEndCallback: (() -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                AndroidTtsVoiceSelector.applyBestUsVoice(engine)
                engine.setPitch(1.0f)
                engine.setSpeechRate(0.95f)
            }
            isTtsReady = true
        } else {
            Log.e("VoiceAssistant", "TTS Initialization failed")
        }
    }

    override fun speak(text: String) {
        val spoken = ForemanVoiceSpeak.prepareSpokenText(text) ?: return
        if (isTtsReady) {
            tts?.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "foreman-${spoken.hashCode()}")
        } else {
            Log.w("VoiceAssistant", "TTS not ready yet")
        }
    }

    override fun stopSpeaking() {
        if (isTtsReady) {
            tts?.stop()
        }
    }

    override fun startListening(onResult: (String) -> Unit, onEnd: () -> Unit) {
        startListeningWithMeta(
            onResult = { meta -> onResult(meta.text) },
            onEnd = onEnd
        )
    }

    override fun startListeningWithMeta(
        onResult: (VoiceTranscriptMeta) -> Unit,
        onEnd: () -> Unit
    ) {
        stopListening()
        onResultCallback = onResult
        onEndCallback = onEnd

        outputFile = File(context.cacheDir, "voice_command.m4a")
        try {
            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                // Strategy B: Hardware noise-suppression and voice pre-processing
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("VoiceAssistant", "Failed to start MediaRecorder", e)
            onEnd()
        }
    }

    override fun stopListening() {
        val recorder = mediaRecorder ?: return
        mediaRecorder = null
        try {
            recorder.stop()
            recorder.release()
        } catch (e: Exception) {
            Log.e("VoiceAssistant", "Failed to stop MediaRecorder", e)
        }

        val file = outputFile ?: return
        if (file.exists()) {
            scope.launch {
                val bytes = file.readBytes()
                // Strategy C: Multi-modal cloud audio processing
                when (val outcome = geminiRepository.transcribeAudio(bytes, "audio/mp4")) {
                    is com.fordham.toolbelt.domain.model.GeminiOutcome.Success -> {
                        val transcript = outcome.text
                        onResultCallback?.invoke(VoiceTranscriptMeta(transcript, 1.0f))
                    }
                    is com.fordham.toolbelt.domain.model.GeminiOutcome.Failure -> {
                        Log.e("VoiceAssistant", "Gemini transcription failed: ${outcome.error.value}")
                    }
                }
                onEndCallback?.invoke()
            }
        } else {
            onEndCallback?.invoke()
        }
    }

    override fun destroy() {
        stopListening()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
