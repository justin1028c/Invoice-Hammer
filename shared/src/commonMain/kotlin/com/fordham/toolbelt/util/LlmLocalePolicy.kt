package com.fordham.toolbelt.util

/**
 * Central locale contract for all Gemini / Foreman LLM calls.
 * Structural prompts stay English; user-visible output follows [AppLocale].
 */
object LlmLocalePolicy {
    enum class OutputMode {
        /** Spoken Foreman replies, summaries, reminder prose. */
        UserFacingProse,
        /** Invoice/receipt/voice JSON extraction — keys English, values localized. */
        StructuredJson,
    }

    fun targetLanguageLabel(): String = AppLocale.fromSystem().geminiLabel

    fun systemInstructionPrefix(mode: OutputMode): String = when (mode) {
        OutputMode.UserFacingProse ->
            "You are Foreman. All user-visible text MUST be in ${targetLanguageLabel()}. " +
                "JSON keys, tool names, and enum values MUST stay in English.\n\n"
        OutputMode.StructuredJson ->
            "JSON keys and schema field names MUST stay in English. " +
                "User-visible string values inside JSON (descriptions, summaries, bodies) " +
                "MUST be in ${targetLanguageLabel()}.\n\n"
    }

    fun wrapSystemInstruction(base: String, mode: OutputMode): String =
        systemInstructionPrefix(mode) + base

    fun wrapPrompt(base: String, mode: OutputMode): String =
        systemInstructionPrefix(mode) + base
}
