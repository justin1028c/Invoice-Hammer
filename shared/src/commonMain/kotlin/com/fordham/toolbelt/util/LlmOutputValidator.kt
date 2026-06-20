package com.fordham.toolbelt.util

/**
 * Validates LLM user-facing prose matches the active app locale.
 * Used as a circuit breaker before showing AI text to users.
 */
object LlmOutputValidator {
    private val spanishSignals = listOf(
        " el ", " la ", " los ", " las ", " de ", " del ", " que ", " por ", " para ",
        " hola", " factura", " cliente", " recordatorio", " debe", " pendiente",
    )

    fun looksSpanish(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase()
        if (text.any { it in "áéíóúñ¿¡" }) return true
        return spanishSignals.any { lower.contains(it) }
    }

    fun matchesLocale(text: String, query: String = ""): Boolean {
        if (AppLocale.fromSystem() != AppLocale.Spanish) return true
        if (query.isNotBlank() && !looksSpanish(query)) return true
        return looksSpanish(text)
    }

    fun ensureUserFacingProse(text: String, fallback: String, query: String = ""): String {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return fallback
        return if (matchesLocale(trimmed, query)) trimmed else fallback
    }
}
