package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase

class GenerateSummaryUseCase(
    private val geminiRepository: GeminiRepository,
    private val hasSubscriptionFeature: HasSubscriptionFeatureUseCase
) {
    suspend operator fun invoke(data: String): GeminiOutcome {
        if (!hasSubscriptionFeature(SubscriptionFeature.AiAgent)) {
            return GeminiOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage("Pro subscription required."))
        }
        return geminiRepository.processTask(TaskType.SUMMARIZE, data)
    }
}
