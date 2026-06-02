package com.fordham.toolbelt.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class AndroidVoiceAssistant(private val context: Context) : TextToSpeech.OnInitListener, VoiceAssistant {

    private var speechRecognizer: SpeechRecognizer? = null
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
        speechRecognizer?.destroy()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5) // Request up to 5 alternative hypotheses
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p0: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(p0: Float) {}
                override fun onBufferReceived(p0: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(p0: Int) {
                    Log.e("VoiceAssistant", "SpeechRec Error code: $p0")
                    onEnd()
                }
                override fun onResults(p0: Bundle?) {
                    val texts = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList<String>()
                    val confidences = p0?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    val text = texts.firstOrNull() ?: ""
                    
                    val rawConfidence = confidences?.firstOrNull() ?: -1.0f
                    // Android engine returns negative values or -1.0f when confidence is unavailable. Null signifies unknown.
                    val confidence = if (rawConfidence >= 0.0f) rawConfidence else null
                    
                    val alternatives = if (texts.size > 1) texts.subList(1, texts.size) else emptyList()
                    
                    Log.d("VoiceAssistant", "SpeechRec Results: $texts, confidence: $confidence, alternatives: $alternatives")
                    if (text.isNotBlank()) {
                        onResult(VoiceTranscriptMeta(text, confidence, alternatives))
                    }
                    onEnd()
                }
                override fun onPartialResults(p0: Bundle?) {}
                override fun onEvent(p0: Int, p1: Bundle?) {}
            })
            startListening(intent)
        }
    }

    override fun stopListening() {
        speechRecognizer?.stopListening()
    }

    override fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

