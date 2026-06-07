package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase
import com.fordham.toolbelt.util.AppLogger

data class ProcessReceiptRequest(
    val imageBytes: ReceiptImagePayload,
    val clientName: ClientName? = null
)

class ProcessReceiptUseCase(
    private val geminiRepository: GeminiRepository,
    private val receiptRepository: ReceiptRepository,
    private val hasSubscriptionFeature: HasSubscriptionFeatureUseCase
) {
    suspend operator fun invoke(request: ProcessReceiptRequest): ProcessReceiptOutcome {
        return try {
            if (!hasSubscriptionFeature(SubscriptionFeature.ReceiptOcr)) {
                ProcessReceiptOutcome.PremiumRequired
            } else {
                val result = geminiRepository.processReceiptImage(request.imageBytes.bytes)
                when (result) {
                    is ReceiptImageOutcome.Success -> {
                        val items = if (request.clientName != null) {
                            result.items.map { it.copy(clientName = request.clientName.value) }
                        } else {
                            result.items
                        }
                        if (items.isEmpty()) {
                            ProcessReceiptOutcome.Failure(
                                FailureMessage("No items could be extracted from this receipt. Please ensure the image is clear and contains readable text.")
                            )
                        } else {
                            receiptRepository.insertItems(items)
                            ProcessReceiptOutcome.Success(items)
                        }
                    }
                    is ReceiptImageOutcome.Failure -> {
                        ProcessReceiptOutcome.Failure(result.error)
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("ProcessReceiptUseCase", "invoke failed", e)
            ProcessReceiptOutcome.Failure(FailureMessage(e.message ?: "Failed to process receipt"))
        }
    }
}
