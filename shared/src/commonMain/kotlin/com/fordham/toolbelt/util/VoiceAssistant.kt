package com.fordham.toolbelt.util

data class VoiceTranscriptMeta(
    val text: String,
    val confidence: Float?,
    val alternatives: List<String> = emptyList()
)

interface VoiceAssistant {
    fun speak(text: String)
    fun stopSpeaking()
    fun startListening(onResult: (String) -> Unit, onEnd: () -> Unit)
    
    // Metadata-aware listening for advanced cross-turn phonetic resolution
    fun startListeningWithMeta(
        onResult: (VoiceTranscriptMeta) -> Unit,
        onEnd: () -> Unit
    ) {
        startListening({ text -> onResult(VoiceTranscriptMeta(text, null)) }, onEnd)
    }

    fun stopListening()
    fun destroy()
}

