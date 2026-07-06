package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.data.implementation.LineItemDescriptionPolishPrompt
import com.fordham.toolbelt.data.local.LocalLlmEngine
import com.fordham.toolbelt.domain.model.GeminiOutcome
import com.fordham.toolbelt.domain.model.LlmPrompt
import com.fordham.toolbelt.util.AppLogger
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.TimeSource

/**
 * Runs a single local-LLM pass over the raw line item descriptions produced by
 * [ParseVoiceInvoiceDeterministicallyUseCase], rewriting them into professional
 * contractor language without touching any amounts, quantities, or tax values.
 *
 * Design constraints:
 * - The LLM receives ONLY description strings. Dollar amounts are never passed in.
 * - If the LLM is unavailable, times out, returns the wrong count, or returns
 *   unparseable JSON → the original descriptions are returned unchanged.
 * - Timeout is 8 seconds. This keeps the voice-to-invoice flow feeling instant.
 * - iOS: [LocalLlmEngine.isSupported] returns false → always falls back to originals.
 */
class PolishLineItemDescriptionsUseCase(
    private val localLlmEngine: LocalLlmEngine
) {
    companion object {
        private const val TAG = "VoiceInvoicePipeline"
        private const val TIMEOUT_MS = 8_000L
        val ActionPrefixPattern = Regex(
            """(?i)^(?:installed|replaced|repaired|fixed|adjusted|patched|painted|caulked|sealed|framed|built|reinforced|reattached|tightened|hauled|removed|pressure\s+washed|completed)\b"""
        )
        val NoisyPhrasePattern = Regex(
            """(?i)\b(?:caught|go wash|to washed?|deer|la|break dollar|dollar deposit|nail pops|sticking|transition strip)\b"""
        )
        val AwkwardBoundaryPattern = Regex("""(?i)^(?:to|for|at|and)\b|\b(?:to|for|at|and|is)$""")
    }

    /**
     * @param rawDescriptions Descriptions from the deterministic parser (may be STT-noisy).
     * @return Polished descriptions in the same order. Falls back to [rawDescriptions] on
     *         any failure without throwing.
     */
    suspend operator fun invoke(rawDescriptions: List<String>): List<String> {
        if (rawDescriptions.isEmpty()) return rawDescriptions
        val candidates = rawDescriptions
            .mapIndexedNotNull { index, description ->
                if (description.needsPolish()) index to description else null
            }
        if (candidates.isEmpty()) {
            AppLogger.d(TAG, "DESCRIPTIONS_POLISH_SKIPPED clean=${rawDescriptions.size}")
            return rawDescriptions
        }

        val available = try {
            localLlmEngine.isSupported()
        } catch (e: Exception) {
            AppLogger.d(TAG, "DESCRIPTIONS_POLISH_SKIPPED local LLM check failed: ${e.message}")
            return rawDescriptions
        }

        if (!available) {
            AppLogger.d(TAG, "DESCRIPTIONS_POLISH_SKIPPED local LLM not available")
            return rawDescriptions
        }

        val startedAt = TimeSource.Monotonic.markNow()
        val candidateDescriptions = candidates.map { it.second }
        val prompt = LlmPrompt(LineItemDescriptionPolishPrompt.build(candidateDescriptions))

        val polished = withTimeoutOrNull(TIMEOUT_MS) {
            when (val outcome = localLlmEngine.generateText(prompt)) {
                is GeminiOutcome.Success -> parsePolishedDescriptions(outcome.text, candidateDescriptions)
                is GeminiOutcome.Failure -> {
                    AppLogger.d(TAG, "DESCRIPTIONS_POLISH_FAILED llm error='${outcome.error.value}'")
                    null
                }
            }
        }

        return if (polished != null) {
            val merged = rawDescriptions.toMutableList()
            candidates.forEachIndexed { polishedIndex, indexedDescription ->
                merged[indexedDescription.first] = polished[polishedIndex]
            }
            AppLogger.d(
                TAG,
                "DESCRIPTIONS_POLISHED candidates=${polished.size}/${rawDescriptions.size} " +
                    "ms=${startedAt.elapsedNow().inWholeMilliseconds} " +
                    polished.mapIndexed { i, d ->
                        "'${candidateDescriptions.getOrElse(i) { "?" }}' -> '$d'"
                    }.joinToString(" | ")
            )
            merged
        } else {
            AppLogger.d(
                TAG,
                "DESCRIPTIONS_POLISH_SKIPPED timeout or null ms=${startedAt.elapsedNow().inWholeMilliseconds} using originals"
            )
            rawDescriptions
        }
    }

    /**
     * Parses the model's JSON array response (e.g. `["Caulked vanity", "Replaced 2 doors"]`).
     * The model is primed to start mid-array (prompt ends with `[\n`), so we reconstruct it.
     * Falls back to [originals] if count mismatches or JSON is malformed.
     */
    private fun parsePolishedDescriptions(
        rawResponse: String,
        originals: List<String>
    ): List<String>? {
        return try {
            // Reconstruct full JSON array: model output starts after the opening `[`
            val fullJson = if (rawResponse.trimStart().startsWith("[")) rawResponse else "[$rawResponse"
            // Close the array if the model didn't
            val closedJson = if (fullJson.trimEnd().endsWith("]")) fullJson else "$fullJson]"

            val results = parseJsonStringArray(closedJson)
            if (results.size != originals.size) {
                AppLogger.d(
                    TAG,
                    "DESCRIPTIONS_POLISH_COUNT_MISMATCH expected=${originals.size} got=${results.size}"
                )
                return null
            }
            // Reject any result that is blank — means the model dropped an item
            if (results.any { it.isBlank() }) {
                AppLogger.d(TAG, "DESCRIPTIONS_POLISH_BLANK_ITEM — using originals")
                return null
            }
            results
        } catch (e: Exception) {
            AppLogger.d(TAG, "DESCRIPTIONS_POLISH_PARSE_ERROR ${e.message} response='$rawResponse'")
            null
        }
    }

    /**
     * Lightweight JSON string-array parser. Avoids a full kotlinx.serialization dependency
     * on a simple array of strings. Handles standard escaped quotes inside strings.
     */
    private fun parseJsonStringArray(json: String): List<String> {
        val trimmed = json.trim()
        require(trimmed.startsWith("[") && trimmed.endsWith("]")) { "Not a JSON array" }
        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        if (inner.isEmpty()) return emptyList()

        val results = mutableListOf<String>()
        var i = 0
        while (i < inner.length) {
            // Skip whitespace and commas between elements
            while (i < inner.length && (inner[i] == ',' || inner[i].isWhitespace())) i++
            if (i >= inner.length) break
            require(inner[i] == '"') { "Expected '\"' at position $i, got '${inner[i]}'" }
            i++ // skip opening quote
            val sb = StringBuilder()
            while (i < inner.length) {
                val c = inner[i]
                if (c == '\\' && i + 1 < inner.length) {
                    when (inner[i + 1]) {
                        '"' -> { sb.append('"'); i += 2 }
                        '\\' -> { sb.append('\\'); i += 2 }
                        'n' -> { sb.append('\n'); i += 2 }
                        else -> { sb.append(inner[i + 1]); i += 2 }
                    }
                } else if (c == '"') {
                    i++ // skip closing quote
                    break
                } else {
                    sb.append(c)
                    i++
                }
            }
            results.add(sb.toString())
        }
        return results
    }

    private fun String.needsPolish(): Boolean {
        val lower = trim().lowercase()
        if (lower.isBlank()) return false
        if (lower.length <= 4) return true
        if (!hasActionPrefix()) return true
        return NoisyPhrasePattern.containsMatchIn(lower) ||
            AwkwardBoundaryPattern.containsMatchIn(lower) ||
            lower.split(Regex("\\s+")).size <= 2
    }

    private fun String.hasActionPrefix(): Boolean =
        ActionPrefixPattern.containsMatchIn(trim())
}
