package com.fordham.toolbelt.data.implementation

object LocalPromptProvider {
    fun getVoiceInvoicePrompt(text: String, currentDate: String): String {
        return VoiceInvoicePromptBuilder.buildLocalPrompt(text, currentDate)
    }
}
