package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.InvoiceTextOutcome
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase
import com.fordham.toolbelt.util.AppLogger
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Responsibility: Extract invoice data from raw text using AI.
 */
class ProcessInvoiceAiUseCase(
    private val geminiRepository: GeminiRepository,
    private val hasSubscriptionFeature: HasSubscriptionFeatureUseCase,
    private val parseVoiceInvoiceDeterministically: ParseVoiceInvoiceDeterministicallyUseCase,
    private val validateVoiceInvoiceResult: ValidateVoiceInvoiceResultUseCase,
    private val extractVoiceInvoiceEvidence: ExtractVoiceInvoiceEvidenceUseCase = ExtractVoiceInvoiceEvidenceUseCase()
) {
    suspend operator fun invoke(text: String, categories: List<String>): InvoiceTextOutcome {
        AppLogger.e(LOG_TAG, "START raw='$text' categories=${categories.joinToString()}")
        if (!hasSubscriptionFeature(SubscriptionFeature.AiAgent)) {
            AppLogger.d(LOG_TAG, "BLOCKED missing AiAgent subscription")
            return InvoiceTextOutcome.Failure(
                FailureMessage("Pro subscription required for invoice AI parsing.")
            )
        }
        val evidence = extractVoiceInvoiceEvidence(text)
        val normalizedText = evidence.normalizedTranscript.ifBlank { text }
        AppLogger.d(
            LOG_TAG,
            "EVIDENCE normalized='$normalizedText' money=${evidence.moneyAmounts.joinToString()} " +
                "percentages=${evidence.percentages.joinToString()} measurements=${evidence.measurements.joinToString()} " +
                "addresses=${evidence.streetAddressCandidates.joinToString()}"
        )
        val deterministic = parseVoiceInvoiceDeterministically(normalizedText)?.let { rawDeterministic ->
            validateVoiceInvoiceResult(rawDeterministic, evidence).also {
                AppLogger.d(
                    LOG_TAG,
                    "DETERMINISTIC_SAFETY_NET ${it.pipelineSummary()} fallback=${it.shouldUseLlmFallback()}"
                )
            }
        }

        AppLogger.d(LOG_TAG, "CALL_GEMMA_FIRST")
        return when (val result = processInvoiceWithGemma(normalizedText, categories)) {
            is InvoiceTextOutcome.Success -> {
                val validated = validateVoiceInvoiceResult(result.result, evidence)
                val useDeterministic = deterministic != null && deterministic.isBetterThan(validated)
                AppLogger.d(
                    LOG_TAG,
                    "GEMMA_FIRST_SUCCESS semantic=${validated.pipelineSummary()} " +
                        "useDeterministicSafetyNet=$useDeterministic"
                )
                InvoiceTextOutcome.Success(if (useDeterministic) deterministic else validated)
            }
            is InvoiceTextOutcome.Failure -> {
                if (deterministic != null) {
                    AppLogger.d(
                        LOG_TAG,
                        "GEMMA_FIRST_FAILURE '${result.error.value}', return deterministic safety net"
                    )
                    InvoiceTextOutcome.Success(deterministic)
                } else {
                    AppLogger.d(LOG_TAG, "GEMMA_FIRST_FAILURE '${result.error.value}' no deterministic parse")
                    result
                }
            }
        }
    }

    private suspend fun processInvoiceWithGemma(text: String, categories: List<String>): InvoiceTextOutcome {
        return try {
            withTimeoutOrNull(LOCAL_GEMMA_TIMEOUT_MS) {
                geminiRepository.processInvoiceText(text, categories)
            } ?: InvoiceTextOutcome.Failure(
                FailureMessage("Local Gemma timed out while parsing that voice invoice.")
            )
        } catch (e: Exception) {
            AppLogger.e(LOG_TAG, "GEMMA_FIRST_EXCEPTION", e)
            InvoiceTextOutcome.Failure(FailureMessage(e.message ?: "Gemma invoice parsing failed."))
        }
    }

    private fun com.fordham.toolbelt.domain.model.AiInvoiceResult.shouldUseLlmFallback(): Boolean {
        if (confidenceScore < 0.85) return true
        if (validationIssues.any { it in RiskyValidationIssues }) return true
        return items.any { item ->
            val description = item.description.value.trim()
            description.length <= 3 ||
                RiskyDescriptionPattern.containsMatchIn(description) ||
                TrailingConnectorPattern.containsMatchIn(description)
        }
    }

    private fun com.fordham.toolbelt.domain.model.AiInvoiceResult.isBetterThan(
        deterministic: com.fordham.toolbelt.domain.model.AiInvoiceResult
    ): Boolean {
        if (items.isEmpty()) return false
        val riskyIssues = validationIssues.count { it in RiskyValidationIssues }
        val deterministicRiskyIssues = deterministic.validationIssues.count { it in RiskyValidationIssues }
        if (riskyIssues != deterministicRiskyIssues) return riskyIssues < deterministicRiskyIssues
        if (validationIssues.size != deterministic.validationIssues.size) return validationIssues.size < deterministic.validationIssues.size
        return confidenceScore > deterministic.confidenceScore && items.size >= deterministic.items.size
    }

    private companion object {
        const val LOG_TAG = "VoiceInvoicePipeline"
        const val LOCAL_GEMMA_TIMEOUT_MS = 15_000L
        val RiskyValidationIssues = setOf(
            "UNMATCHED_MONEY_AMOUNT",
            "NO_VALID_LINE_ITEMS",
            "ZERO_AMOUNT",
            "LOW_AUDIO_CONFIDENCE",
            "MATH_MISMATCH"
        )
        val RiskyDescriptionPattern = Regex("""(?i)^(?:and|for|at)\b|\b(?:\bla\b|deer|dollar deposit|break dollar)\b""")
        val TrailingConnectorPattern = Regex("""(?i)\b(?:and|for|at|is)$""")
    }
}

private fun com.fordham.toolbelt.domain.model.AiInvoiceResult.pipelineSummary(): String =
    "client='$clientName' address='$clientAddress' items=${items.size} " +
        "subtotal=${items.sumOf { it.amount.value }} confidence=$confidenceScore issues=$validationIssues " +
        "deposit=$depositAmount tax=$taxRatePercent lines=[" +
        items.joinToString(" | ") {
            "${it.description.value}:${it.amount.value}:qty=${it.quantity}:unit=${it.unitPrice?.value}:cat=${it.category}"
        } +
        "]"
