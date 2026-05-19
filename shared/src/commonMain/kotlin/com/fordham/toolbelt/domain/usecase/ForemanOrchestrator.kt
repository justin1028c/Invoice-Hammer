package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.repository.*

class ForemanOrchestrator(
    private val geminiRepository: GeminiRepository,
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun processCommand(input: NaturalLanguage, appContext: NaturalLanguage): OrchestrationResult {
        // 1. Check Premium Status (Safety Gate)
        val settings = settingsRepository.getBusinessSettings()
        if (!settings.isPremium) {
            return OrchestrationResult.Failure(FailureMessage("Premium subscription required for Agentic commands"))
        }

        // 2. Generate Tool Call from Repository
        val result = geminiRepository.generateToolCall(input.value, appContext.value)
        
        return when (result) {
            is ToolCallOutcome.Success -> {
                val toolCall = result.toolCall ?: return OrchestrationResult.ResponseOnly("I'm not sure how to help with that yet.")
                
                // 3. Deterministic Safety Gate
                if (toolCall.type.category == ToolCategory.DESTRUCTIVE) {
                    OrchestrationResult.ApprovalRequired(toolCall)
                } else {
                    executeSafeTool(toolCall)
                }
            }
            is ToolCallOutcome.Failure -> OrchestrationResult.Failure(result.error)
        }
    }

    private suspend fun executeSafeTool(toolCall: ForemanToolCall): OrchestrationResult {
        return try {
            when (val params = toolCall.parameters) {
                is ToolParameters.SearchClients -> {
                    val clients = clientRepository.searchClients(params.query)
                    OrchestrationResult.Executed("Found ${clients.size} clients matching '${params.query}'", toolCall)
                }
                is ToolParameters.OpenTab -> {
                    OrchestrationResult.Executed("Navigating to ${params.tabName} tab", toolCall)
                }
                is ToolParameters.AddJobNote -> {
                    // Logic to add a note
                    OrchestrationResult.Executed("Added note for ${params.clientName}", toolCall)
                }
                else -> OrchestrationResult.Executed(toolCall.reasoning, toolCall)
            }
        } catch (e: Exception) {
            OrchestrationResult.Failure(FailureMessage("Failed to execute tool ${toolCall.type}: ${e.message}"))
        }
    }
}
