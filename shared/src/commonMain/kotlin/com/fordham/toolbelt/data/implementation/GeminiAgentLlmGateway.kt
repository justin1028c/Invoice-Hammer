package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.ToolCallOutcome
import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.ForemanRuntimeBinding
import com.fordham.toolbelt.domain.model.agent.ForemanPromptComposer
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.repository.AgentLlmGateway
import com.fordham.toolbelt.domain.repository.ForemanJobMemoryPort
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.util.ForemanMessageLocalizer
import com.fordham.toolbelt.util.LlmOutputValidator

class GeminiAgentLlmGateway(
    private val geminiRepository: GeminiRepository,
    private val jobMemory: ForemanJobMemoryPort,
    private val toolCallMapper: ForemanToolCallMapper,
    private val settingsRepository: SettingsRepository
) : AgentLlmGateway {

    override suspend fun prompt(
        systemPrompt: NaturalLanguage,
        session: ForemanSession,
        functions: List<AgentFunction>
    ): AgentOutcome {
        if (functions.isEmpty()) {
            return AgentOutcome.Failure(
                FailureMessage("Foreman tools are unavailable. Restart the app and try again.")
            )
        }

        val autoSaveEnabled = settingsRepository.getBusinessSettings().autoSaveVoiceInvoices
        val planner = ForemanPromptComposer.compose(
            systemPrompt = systemPrompt,
            session = session,
            jobMemory = jobMemory,
            autoSaveEnabled = autoSaveEnabled
        )
        if (planner.userInput.isBlank()) {
            return AgentOutcome.Failure(FailureMessage("Agent command cannot be blank."))
        }

        return when (
            val result = geminiRepository.generateToolCall(
                input = planner.userInput,
                context = planner.contextBlock,
                session = session,
                functions = functions,
                imageBytes = ForemanRuntimeBinding.current().pendingReceiptImageBytes,
                systemInstruction = planner.systemInstruction,
                toolCallingMode = planner.toolCallingMode
            )
        ) {
            is ToolCallOutcome.Failure -> AgentOutcome.Failure(result.error)
            is ToolCallOutcome.Success -> {
                val toolCall = result.toolCall
                if (toolCall == null) {
                    val raw = result.completionReasoning.ifBlank { "Done." }
                    val localized = ForemanMessageLocalizer.localize(raw)
                    val message = LlmOutputValidator.ensureUserFacingProse(
                        localized,
                        ForemanMessageLocalizer.localize("Done."),
                        planner.userInput
                    )
                    return AgentOutcome.TextResponse(NaturalLanguage(message))
                }
                toolCallMapper.toAgentOutcome(toolCall)
            }
        }
    }
}
