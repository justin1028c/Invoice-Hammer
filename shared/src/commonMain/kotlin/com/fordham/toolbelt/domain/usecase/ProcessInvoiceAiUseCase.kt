package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.InvoiceTextOutcome
import com.fordham.toolbelt.domain.repository.GeminiRepository

/**
 * Responsibility: Extract invoice data from raw text using AI.
 */
class ProcessInvoiceAiUseCase(
    private val geminiRepository: GeminiRepository
) {
    suspend operator fun invoke(text: String, categories: List<String>): InvoiceTextOutcome {
        return geminiRepository.processInvoiceText(text, categories)
    }
}
