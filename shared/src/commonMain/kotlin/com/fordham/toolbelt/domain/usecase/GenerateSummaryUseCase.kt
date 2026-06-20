package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase
import com.fordham.toolbelt.util.AppLocale
import com.fordham.toolbelt.util.LlmOutputValidator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GenerateSummaryUseCase(
    private val geminiRepository: GeminiRepository,
    private val hasSubscriptionFeature: HasSubscriptionFeatureUseCase
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(data: String): GeminiOutcome {
        if (!hasSubscriptionFeature(SubscriptionFeature.AiAgent)) {
            return GeminiOutcome.Failure(FailureMessage("Pro subscription required."))
        }
        return when (val outcome = geminiRepository.processTask(TaskType.SUMMARIZE, data)) {
            is GeminiOutcome.Success -> GeminiOutcome.Success(extractValidatedSummary(outcome.text))
            is GeminiOutcome.Failure -> outcome
        }
    }

    private fun extractValidatedSummary(rawJson: String): String {
        val summary = runCatching {
            json.parseToJsonElement(rawJson).jsonObject["summary"]?.jsonPrimitive?.content
        }.getOrNull()?.trim().orEmpty()
        val fallback = if (AppLocale.fromSystem() == AppLocale.Spanish) {
            "Resumen no disponible."
        } else {
            "Summary unavailable."
        }
        return LlmOutputValidator.ensureUserFacingProse(summary, fallback)
    }
}
