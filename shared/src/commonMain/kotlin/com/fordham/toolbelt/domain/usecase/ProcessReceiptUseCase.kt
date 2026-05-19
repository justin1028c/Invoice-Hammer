package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

data class ProcessReceiptRequest(
    val imageBytes: ByteArray,
    val clientName: String? = null
)

class ProcessReceiptUseCase(
    private val geminiRepository: GeminiRepository,
    private val receiptRepository: ReceiptRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(request: ProcessReceiptRequest): ProcessReceiptOutcome {
        return try {
            val settings = settingsRepository.businessSettingsFlow.first()
            if (!settings.isPremium) {
                ProcessReceiptOutcome.PremiumRequired
            } else {
                val result = geminiRepository.processReceiptImage(request.imageBytes)
                when (result) {
                    is ReceiptImageOutcome.Success -> {
                        val items = if (request.clientName != null) {
                            result.items.map { it.copy(clientName = request.clientName) }
                        } else {
                            result.items
                        }
                        receiptRepository.insertItems(items)
                        ProcessReceiptOutcome.Success(items)
                    }
                    is ReceiptImageOutcome.Failure -> {
                        ProcessReceiptOutcome.Failure(result.error)
                    }
                }
            }
        } catch (e: Exception) {
            ProcessReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to process receipt"))
        }
    }
}
