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

    // Multi-segment silence-aware state tracking variables
    private var accumulatedText = ""
    private var lastSpeechTimestamp = 0L
    private var isFinalizing = false

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
        accumulatedText = ""
        isFinalizing = false
        lastSpeechTimestamp = System.currentTimeMillis()
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
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)
                        val locale = if (AppLocale.fromSystem() == AppLocale.Spanish) "es-ES" else "en-US"
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
                        // Standard values (may be ignored by Google recognition engine, hence our loop wrapper)
                        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                    }

                    val sessionId = currentSessionId

                    // Launch or resume the absolute silence checking coroutine
                    if (silenceJob == null || silenceJob?.isCompleted == true) {
                        silenceJob = scope.launch(Dispatchers.Main) {
                            val checkInterval = 200L
                            val maxSilenceMs = 3500L
                            while (currentSessionId == sessionId && !isFinalizing) {
                                delay(checkInterval)
                                val elapsed = System.currentTimeMillis() - lastSpeechTimestamp
                                if (elapsed >= maxSilenceMs) {
                                    Log.d("VoiceAssistant", "Silence limit of ${maxSilenceMs}ms reached, finalizing speech recognition.")
                                    isFinalizing = true
                                    stopListening(discard = false)
                                    break
                                }
                            }
                        }
                    }

                    recognizer.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d("VoiceAssistant", "SpeechRecognizer: onReadyForSpeech")
                            if (sessionId == currentSessionId) {
                                lastSpeechTimestamp = System.currentTimeMillis()
                            }
                        }
                        override fun onBeginningOfSpeech() {
                            Log.d("VoiceAssistant", "SpeechRecognizer: onBeginningOfSpeech")
                            if (sessionId == currentSessionId) {
                                lastSpeechTimestamp = System.currentTimeMillis()
                            }
                        }
                        override fun onRmsChanged(rmsdB: Float) {
                            // If rmsdB is elevated, treat it as active input activity
                            if (rmsdB > 3.0f && sessionId == currentSessionId) {
                                lastSpeechTimestamp = System.currentTimeMillis()
                            }
                        }
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
                                val elapsed = System.currentTimeMillis() - lastSpeechTimestamp
                                val isTransient = error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                                                  error == SpeechRecognizer.ERROR_NO_MATCH ||
                                                  error == SpeechRecognizer.ERROR_NETWORK ||
                                                  error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                                                  error == SpeechRecognizer.ERROR_CLIENT

                                if (!isFinalizing && elapsed < 3500L && isTransient) {
                                    Log.d("VoiceAssistant", "Restarting recognizer due to transient/silence error: $msg")
                                    scope.launch(Dispatchers.Main) {
                                        delay(100)
                                        if (sessionId == currentSessionId && !isFinalizing) {
                                            startSpeechRecognizerListening(onResult, onEnd, onPartialResult)
                                        }
                                    }
                                } else {
                                    isFinalizing = true
                                    val finalCombined = accumulatedText.trim()
                                    onResult(
                                        VoiceTranscriptMeta(
                                            text = finalCombined,
                                            confidence = if (finalCombined.isBlank()) 0.0f else 0.55f,
                                            alternatives = emptyList()
                                        )
                                    )
                                    onEnd()
                                }
                            }
                        }
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                            val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                            val ranked = rankSpeechCandidates(matches, confidences)
                            val text = ranked.firstOrNull()?.text.orEmpty()
                            Log.d(
                                "VoiceAssistant",
                                "SpeechRecognizer ranked results: ${ranked.take(5).joinToString(" | ") { "${it.text} (${it.confidence})" }}"
                            )
                            isRecognizerListening = false

                            if (sessionId == currentSessionId) {
                                val finalCombined = if (accumulatedText.isEmpty()) {
                                    text
                                } else {
                                    if (text.isNotEmpty()) "$accumulatedText $text" else accumulatedText
                                }

                                val elapsed = System.currentTimeMillis() - lastSpeechTimestamp
                                if (isFinalizing || elapsed >= 3500L) {
                                    isFinalizing = true
                                    onResult(
                                        VoiceTranscriptMeta(
                                            text = finalCombined.trim(),
                                            confidence = ranked.firstOrNull()?.confidence,
                                            alternatives = buildCombinedAlternatives(accumulatedText, ranked.map { it.text }, finalCombined)
                                        )
                                    )
                                    onEnd()
                                } else {
                                    accumulatedText = finalCombined
                                    onPartialResult(finalCombined)
                                    Log.d("VoiceAssistant", "Segment complete. Restarting recognizer. Current accumulated: $accumulatedText")
                                    scope.launch(Dispatchers.Main) {
                                        if (sessionId == currentSessionId && !isFinalizing) {
                                            startSpeechRecognizerListening(onResult, onEnd, onPartialResult)
                                        }
                                    }
                                }
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotBlank() && sessionId == currentSessionId) {
                                Log.d("VoiceAssistant", "SpeechRecognizer partial results: $text")
                                lastSpeechTimestamp = System.currentTimeMillis()
                                if (accumulatedText.isNotEmpty() && isTinyRestartFragment(text)) {
                                    return
                                }
                                val combined = if (accumulatedText.isEmpty()) text else "$accumulatedText $text"
                                onPartialResult(combined)
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

    private data class SpeechCandidate(
        val text: String,
        val confidence: Float?,
        val score: Float
    )

    private fun rankSpeechCandidates(
        matches: List<String>,
        confidences: FloatArray?
    ): List<SpeechCandidate> {
        return matches
            .mapIndexedNotNull { index, raw ->
                val text = raw.trim()
                if (text.isBlank()) return@mapIndexedNotNull null
                val confidence = confidences?.getOrNull(index)?.takeIf { it >= 0f }
                SpeechCandidate(
                    text = text,
                    confidence = confidence,
                    score = invoiceSpeechScore(text) + ((confidence ?: 0.5f) * 3f) - (index * 0.15f)
                )
            }
            .sortedByDescending { it.score }
    }

    private fun buildCombinedAlternatives(
        prefix: String,
        candidates: List<String>,
        selected: String
    ): List<String> {
        return candidates
            .map { candidate ->
                if (prefix.isBlank()) candidate else "$prefix $candidate"
            }
            .map { it.trim() }
            .filter { it.isNotBlank() && it != selected.trim() }
            .distinct()
            .take(5)
    }

    private fun invoiceSpeechScore(text: String): Float {
        val lower = text.lowercase()
        var score = 0f

        score += Regex("""\$\s*\d+|\b\d+(?:\.\d+)?\s*(?:dollars?|bucks?|cents?)\b""").findAll(lower).count() * 2.2f
        score += Regex("""\b\d+\s*(?:each|per|x|times|ft|feet|foot|sq\s*ft|square\s+feet|hours?|linear\s+feet)\b""").findAll(lower).count() * 1.5f
        score += Regex("""\b(?:client|customer|for|at|address|invoice|estimate|charge|tax|deposit|paid)\b""").findAll(lower).count() * 1.0f
        score += Regex("""\b(?:install(?:ed)?|replace(?:d)?|repair(?:ed)?|paint(?:ed)?|haul(?:ed)?|remove(?:d)?|pressure\s+wash(?:ed)?|drywall|trim|valves?|outlets?|labor|material)\b""").findAll(lower).count() * 1.2f
        score += Regex("""\b\d{5}(?:-\d{4})?\b""").findAll(lower).count() * 1.4f
        score += Regex("""\b\d+\s+[a-z0-9 .'-]+(?:street|st|road|rd|drive|dr|court|ct|lane|ln|avenue|ave|boulevard|blvd|way|circle|cir)\b""").findAll(lower).count() * 2.0f

        if (Regex("""\bto\s+shut\s+off\s+valves?\b""").containsMatchIn(lower)) score -= 1.5f
        if (Regex("""\bto\s+(?:nail\s+pops?|outlets?|receptacles?|valves?|boards?|fixtures?)\b""").containsMatchIn(lower)) score -= 1.5f
        if (Regex("""\btwo\s+(?:nail\s+pops?|outlets?|receptacles?|valves?|boards?|fixtures?)\b""").containsMatchIn(lower)) score += 1.2f
        if (Regex("""\bgo\s+wash(?:ed)?\b""").containsMatchIn(lower)) score -= 1.0f
        if (Regex("""\baway\s+the\s+old\s+unit\b""").containsMatchIn(lower)) score -= 0.8f
        // "pass material(s)" is a STT mishear of "patch materials"
        if (Regex("""\bpass\s+materials?\b""").containsMatchIn(lower)) score -= 1.5f
        if (Regex("""\bpatch\s+materials?\b""").containsMatchIn(lower)) score += 1.2f

        return score
    }

    private fun isTinyRestartFragment(text: String): Boolean {
        val trimmed = text.trim()
        if (Regex("""\$\s*\d+|\b\d+(?:\.\d+)?\b""").containsMatchIn(trimmed)) return false
        return trimmed.split(Regex("""\s+""")).size <= 2
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
                val requiredSilenceDurationMs = 3000L
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

        if (!discard) {
            isFinalizing = true
        } else {
            currentSessionId++
            accumulatedText = ""
            isFinalizing = false
        }

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
            scope.launch(Dispatchers.Default) {
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
