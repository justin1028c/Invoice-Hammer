package com.fordham.toolbelt.util

interface VoiceAssistant {
    fun speak(text: String)
    fun startListening(onResult: (String) -> Unit, onEnd: () -> Unit)
    fun stopListening()
    fun destroy()
}
