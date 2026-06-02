package com.fordham.toolbelt.util

/**
 * Normalizes Foreman replies before on-device TTS (Tier 1 — free system voices).
 */
object ForemanVoiceSpeak {
    /** Keeps job-site playback short and natural. */
    const val MAX_SPEAK_CHARS: Int = 220

    fun prepareSpokenText(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        val singleLine = trimmed.replace('\n', ' ').replace(Regex("\\s+"), " ")
        if (singleLine.length <= MAX_SPEAK_CHARS) return singleLine
        val cut = singleLine.take(MAX_SPEAK_CHARS)
        val lastStop = maxOf(cut.lastIndexOf('.'), cut.lastIndexOf('!'), cut.lastIndexOf('?'))
        return if (lastStop > 40) cut.take(lastStop + 1).trim() else "$cut…"
    }
}
