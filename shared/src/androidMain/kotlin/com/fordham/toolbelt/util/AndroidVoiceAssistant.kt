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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.content.Intent
import android.os.Bundle

class AndroidVoiceAssistant(
    private val context: Context,
    private val geminiRepository: GeminiRepository
) : TextToSpeech.OnInitListener, VoiceAssistant {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var onResultCallback: ((VoiceTranscriptMeta) -> Unit)? = null
    private var onEndCallback: (() -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var currentSessionId = 0
    private var silenceJob: Job? = null

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecognizerListening = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                AndroidTtsVoiceSelector.applyBestVoice(engine, AppLocale.fromSystem())
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

    override fun startListening(
        onResult: (String) -> Unit,
        onEnd: () -> Unit,
        onPartialResult: (String) -> Unit
    ) {
        startListeningWithMeta(
            onResult = { meta -> onResult(meta.text) },
            onEnd = onEnd,
            onPartialResult = onPartialResult
        )
    }

    override fun startListeningWithMeta(
        onResult: (VoiceTranscriptMeta) -> Unit,
        onEnd: () -> Unit,
        onPartialResult: (String) -> Unit
    ) {
        onResultCallback = null
        onEndCallback = null
        stopListening(discard = true)

        currentSessionId++
        onResultCallback = onResult
        onEndCallback = onEnd

        scope.launch(Dispatchers.Main) {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                startSpeechRecognizerListening(onResult, onEnd, onPartialResult)
            } else {
                startMediaRecorderListening(onResult, onEnd)
            }
        }
    }

    private fun initSpeechRecognizer(onReady: (SpeechRecognizer) -> Unit) {
        if (speechRecognizer != null) {
            onReady(speechRecognizer!!)
            return
        }
        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            onReady(recognizer)
        } catch (e: Exception) {
            Log.e("VoiceAssistant", "Failed to create SpeechRecognizer", e)
        }
    }

    private fun startSpeechRecognizerListening(
        onResult: (VoiceTranscriptMeta) -> Unit,
        onEnd: () -> Unit,
        onPartialResult: (String) -> Unit
    ) {
        initSpeechRecognizer { recognizer ->
            scope.launch(Dispatchers.Main) {
                try {
                    recognizer.cancel()
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                        val locale = if (AppLocale.fromSystem() == AppLocale.Spanish) "es-ES" else "en-US"
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
                    }

                    val sessionId = currentSessionId
                    recognizer.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d("VoiceAssistant", "SpeechRecognizer: onReadyForSpeech")
                        }
                        override fun onBeginningOfSpeech() {
                            Log.d("VoiceAssistant", "SpeechRecognizer: onBeginningOfSpeech")
                        }
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            Log.d("VoiceAssistant", "SpeechRecognizer: onEndOfSpeech")
                        }
                        override fun onError(error: Int) {
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "SpeechRecognizer is busy"
                                SpeechRecognizer.ERROR_SERVER -> "Server error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                                else -> "Unknown speech recognizer error ($error)"
                            }
                            Log.e("VoiceAssistant", "SpeechRecognizer error: $msg")
                            isRecognizerListening = false
                            if (sessionId == currentSessionId) {
                                onEnd()
                            }
                        }
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            Log.d("VoiceAssistant", "SpeechRecognizer results: $text")
                            isRecognizerListening = false
                            if (sessionId == currentSessionId) {
                                onResult(VoiceTranscriptMeta(text, 1.0f))
                                onEnd()
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotBlank() && sessionId == currentSessionId) {
                                Log.d("VoiceAssistant", "SpeechRecognizer partial results: $text")
                                onPartialResult(text)
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })

                    recognizer.startListening(intent)
                    isRecognizerListening = true
                } catch (e: Exception) {
                    Log.e("VoiceAssistant", "Error starting SpeechRecognizer", e)
                    isRecognizerListening = false
                    startMediaRecorderListening(onResult, onEnd)
                }
            }
        }
    }

    private fun startMediaRecorderListening(
        onResult: (VoiceTranscriptMeta) -> Unit,
        onEnd: () -> Unit
    ) {
        outputFile = File(context.cacheDir, "voice_command.m4a")
        try {
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder

            silenceJob = scope.launch(Dispatchers.Main) {
                val silenceThreshold = 1200
                val checkIntervalMs = 200L
                val requiredSilenceDurationMs = 1200L
                var consecutiveSilenceMs = 0L

                delay(1000)

                while (mediaRecorder != null) {
                    delay(checkIntervalMs)
                    val amplitude = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    if (amplitude < silenceThreshold) {
                        consecutiveSilenceMs += checkIntervalMs
                        if (consecutiveSilenceMs >= requiredSilenceDurationMs) {
                            stopListening(discard = false)
                            break
                        }
                    } else {
                        consecutiveSilenceMs = 0L
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceAssistant", "Failed to start MediaRecorder", e)
            onEnd()
        }
    }

    override fun stopListening(discard: Boolean) {
        silenceJob?.cancel()
        silenceJob = null

        if (isRecognizerListening) {
            isRecognizerListening = false
            scope.launch(Dispatchers.Main) {
                if (discard) {
                    speechRecognizer?.cancel()
                } else {
                    speechRecognizer?.stopListening()
                }
            }
            return
        }

        if (discard) {
            currentSessionId++
        }
        val recorder = mediaRecorder
        if (recorder == null) {
            if (discard) {
                onResultCallback = null
                onEndCallback = null
            }
            return
        }
        mediaRecorder = null

        val sessionId = currentSessionId
        val localResultCallback = onResultCallback
        val localEndCallback = onEndCallback

        onResultCallback = null
        onEndCallback = null

        try {
            recorder.stop()
            recorder.release()
        } catch (e: Exception) {
            Log.e("VoiceAssistant", "Failed to stop MediaRecorder", e)
        }

        if (discard) {
            outputFile?.delete()
            localEndCallback?.invoke()
            return
        }

        val file = outputFile ?: return
        if (file.exists()) {
            scope.launch {
                val bytes = file.readBytes()
                when (val outcome = geminiRepository.transcribeAudio(bytes, "audio/mp4")) {
                    is com.fordham.toolbelt.domain.model.GeminiOutcome.Success -> {
                        val transcript = outcome.text
                        if (sessionId == currentSessionId) {
                            localResultCallback?.invoke(VoiceTranscriptMeta(transcript, 1.0f))
                        }
                    }
                    is com.fordham.toolbelt.domain.model.GeminiOutcome.Failure -> {
                        Log.e("VoiceAssistant", "Gemini transcription failed: ${outcome.error.value}")
                    }
                }
                if (sessionId == currentSessionId) {
                    localEndCallback?.invoke()
                }
            }
        } else {
            if (sessionId == currentSessionId) {
                localEndCallback?.invoke()
            }
        }
    }

    override fun destroy() {
        silenceJob?.cancel()
        silenceJob = null
        stopListening(discard = true)
        scope.launch(Dispatchers.Main) {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
