package com.fordham.toolbelt.util

import platform.AVFoundation.AVSpeechSynthesisVoice
import platform.AVFoundation.AVSpeechSynthesisVoiceQualityEnhanced
import com.fordham.toolbelt.util.AppLogger

internal object IosTtsVoiceSelector {
    fun bestUsVoice(): AVSpeechSynthesisVoice? {
        val voices = AVSpeechSynthesisVoice.speechVoices()
        var best: AVSpeechSynthesisVoice? = null
        var bestScore = Int.MIN_VALUE
        for (i in 0 until voices.size.toInt()) {
            val voice = voices[i] as? AVSpeechSynthesisVoice ?: continue
            val language = voice.language as? String ?: continue
            if (!language.startsWith("en")) continue
            var score = 0
            if (voice.quality == AVSpeechSynthesisVoiceQualityEnhanced) score += 100
            if (language == "en-US") score += 25
            if (score > bestScore) {
                bestScore = score
                best = voice
            }
        }
        best?.let {
            AppLogger.d("VoiceAssistant", "TTS voice=${it.name} lang=${it.language} quality=${it.quality}")
        }
        return best
    }
}
