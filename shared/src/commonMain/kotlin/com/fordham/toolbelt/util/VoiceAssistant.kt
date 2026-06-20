package com.fordham.toolbelt.util

data class VoiceTranscriptMeta(
    val text: String,
    val confidence: Float?,
    val alternatives: List<String> = emptyList()
)

interface VoiceAssistant {
    fun speak(text: String)
    fun stopSpeaking()
    fun startListening(
        onResult: (String) -> Unit,
        onEnd: () -> Unit,
        onPartialResult: (String) -> Unit = {}
    )
    
    // Metadata-aware listening for advanced cross-turn phonetic resolution
    fun startListeningWithMeta(
        onResult: (VoiceTranscriptMeta) -> Unit,
        onEnd: () -> Unit,
        onPartialResult: (String) -> Unit = {}
    ) {
        startListening(
            onResult = { text -> onResult(VoiceTranscriptMeta(text, null)) },
            onEnd = onEnd,
            onPartialResult = onPartialResult
        )
    }

    fun stopListening(discard: Boolean = false)
    fun destroy()
}

