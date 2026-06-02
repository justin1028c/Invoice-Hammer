package com.fordham.toolbelt.util

import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.fordham.toolbelt.util.AppLogger
import java.util.Locale

internal object AndroidTtsVoiceSelector {
    fun applyBestUsVoice(tts: TextToSpeech) {
        tts.language = Locale.US
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        val voices = tts.voices ?: return
        val best = voices
            .asSequence()
            .filter { voice -> isUsEnglish(voice) }
            .maxByOrNull { score(it) }
        if (best != null) {
            val applied = tts.setVoice(best)
            if (applied == TextToSpeech.SUCCESS) {
                AppLogger.d("VoiceAssistant", "TTS voice=${best.name} quality=${best.quality} network=${best.isNetworkConnectionRequired}")
            }
        }
    }

    private fun isUsEnglish(voice: Voice): Boolean {
        val loc = voice.locale ?: return false
        return loc.language == "en" && (loc.country.isNullOrEmpty() || loc.country.equals("US", ignoreCase = true))
    }

    private fun score(voice: Voice): Int {
        var s = voice.quality
        val name = voice.name.lowercase()
        if (name.contains("neural") || name.contains("network") || name.contains("wavenet")) s += 80
        if (voice.quality >= Voice.QUALITY_VERY_HIGH) s += 40
        else if (voice.quality >= Voice.QUALITY_HIGH) s += 20
        if (!voice.isNetworkConnectionRequired) s += 15
        return s
    }
}
