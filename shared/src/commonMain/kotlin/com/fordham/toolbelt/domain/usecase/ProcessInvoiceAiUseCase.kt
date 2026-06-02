package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.InvoiceTextOutcome
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase

/**
 * Responsibility: Extract invoice data from raw text using AI.
 */
class ProcessInvoiceAiUseCase(
    private val geminiRepository: GeminiRepository,
    private val hasSubscriptionFeature: HasSubscriptionFeatureUseCase
) {
    suspend operator fun invoke(text: String, categories: List<String>): InvoiceTextOutcome {
        if (!hasSubscriptionFeature(SubscriptionFeature.AiAgent)) {
            return InvoiceTextOutcome.Failure(
                FailureMessage("Pro subscription required for invoice AI parsing.")
            )
        }
        return geminiRepository.processInvoiceText(text, categories)
    }
}
