package com.fordham.toolbelt.domain.usecase.agent

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.JobNoteRepository
import com.fordham.toolbelt.domain.repository.GeminiRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface DetectChangeOrdersOutcome {
    data class Success(val opportunities: List<ChangeOrderOpportunity>) : DetectChangeOrdersOutcome
    data class ProjectNotFound(val invoiceId: InvoiceId) : DetectChangeOrdersOutcome
    data class Error(val message: FailureMessage) : DetectChangeOrdersOutcome
}

class DetectChangeOrdersUseCase(
    private val invoiceRepository: InvoiceRepository,
    private val jobNoteRepository: JobNoteRepository,
    private val geminiRepository: GeminiRepository
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    suspend operator fun invoke(invoiceId: InvoiceId): DetectChangeOrdersOutcome {
        return try {
            val invoice = invoiceRepository.getInvoiceById(invoiceId)
                ?: return DetectChangeOrdersOutcome.ProjectNotFound(invoiceId)

            // 1. Fetch system budget to see what was estimated
            val notes = jobNoteRepository.getNotesByInvoice(invoiceId).first()
            val budgetNote = notes.firstOrNull { it.text.startsWith("[SYSTEM_BUDGET]") }
            val parsedBudget = budgetNote?.let { SystemBudgetSerializer.deserialize(it.text) }
            val budgetedItems = parsedBudget?.lineItems ?: emptyList()

            // 2. Fetch recent contractor logs (filter out system budget notes)
            val userLogs = notes.filter { !it.text.startsWith("[SYSTEM_BUDGET]") && it.text.isNotBlank() }
            if (userLogs.isEmpty()) {
                return DetectChangeOrdersOutcome.Success(emptyList())
            }

            // 3. Compile prompt context payload
            val dataPayload = buildString {
                append("Original Estimate Items:\n")
                if (budgetedItems.isEmpty()) {
                    append("- None listed (Standard Invoice / Direct Billing)\n")
                } else {
                    budgetedItems.forEach { item ->
                        append("- ${item.description} (Category: ${item.category}, Amount: $${item.amount})\n")
                    }
                }
                append("\nRecent Job logs / Voice Transcripts:\n")
                userLogs.forEach { log ->
                    append("- [${log.formattedDate}] ${log.text}\n")
                }
            }

            // 4. Request Gemini analysis
            when (val outcome = geminiRepository.processTask(TaskType.DETECT_CHANGE_ORDERS, dataPayload)) {
                is GeminiOutcome.Failure -> {
                    DetectChangeOrdersOutcome.Error(outcome.error)
                }
                is GeminiOutcome.Success -> {
                    val parsed = json.decodeFromString<LlmChangeOrderResponseDto>(outcome.text)
                    val opportunities = parsed.opportunities.map { dto ->
                        val confidence = try {
                            OpportunityConfidence.valueOf(dto.confidence.uppercase())
                        } catch (e: Exception) {
                            OpportunityConfidence.MEDIUM
                        }
                        val recommended = dto.recommendedItems.map { itemDto ->
                            LineItem(
                                description = itemDto.description,
                                amount = itemDto.amount,
                                category = itemDto.category
                            )
                        }
                        ChangeOrderOpportunity(
                            invoiceId = invoiceId,
                            clientName = ClientName(invoice.clientName),
                            detectedTask = NaturalLanguage(dto.detectedTask),
                            recommendedItems = recommended,
                            estimatedValueRange = dto.minPrice..dto.maxPrice,
                            confidence = confidence
                        )
                    }
                    DetectChangeOrdersOutcome.Success(opportunities)
                }
            }
        } catch (e: Exception) {
            com.fordham.toolbelt.util.AppLogger.e("DetectChangeOrdersUseCase", "Change order detection failed", e)
            DetectChangeOrdersOutcome.Error(FailureMessage(e.message ?: "Change order analysis failed."))
        }
    }
}

@Serializable
private data class LlmLineItemDto(
    val description: String,
    val amount: Double,
    val category: String = "Service"
)

@Serializable
private data class LlmOpportunityDto(
    val detectedTask: String,
    val confidence: String,
    val minPrice: Double,
    val maxPrice: Double,
    val recommendedItems: List<LlmLineItemDto>
)

@Serializable
private data class LlmChangeOrderResponseDto(
    val opportunities: List<LlmOpportunityDto>
)
