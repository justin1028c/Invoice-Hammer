package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.ToolParameters
import com.fordham.toolbelt.domain.model.ToolType
import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.CreateDraftInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.DeleteInvoiceApprovalArgs
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.SearchClientsArgs
import com.fordham.toolbelt.domain.model.agent.ToolCallId
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.repository.AgentLlmGateway
import com.fordham.toolbelt.domain.repository.ClientRepository
import com.fordham.toolbelt.domain.repository.GeminiRepository

class GeminiAgentLlmGateway(
    private val geminiRepository: GeminiRepository,
    private val clientRepository: ClientRepository
) : AgentLlmGateway {
    override suspend fun prompt(
        systemPrompt: NaturalLanguage,
        session: ForemanSession,
        functions: List<AgentFunction>
    ): AgentOutcome {
        val command = session.history.lastOrNull()?.content?.value.orEmpty()
        if (command.isBlank()) {
            return AgentOutcome.Failure(FailureMessage("Agent command cannot be blank."))
        }

        return when (val result = geminiRepository.generateToolCall(command, systemPrompt.value)) {
            is com.fordham.toolbelt.domain.model.ToolCallOutcome.Failure -> AgentOutcome.Failure(result.error)
            is com.fordham.toolbelt.domain.model.ToolCallOutcome.Success -> {
                val toolCall = result.toolCall ?: return AgentOutcome.TextResponse(
                    NaturalLanguage("I can help with invoices, clients, receipts, and job history. Try asking me to find a client or start a draft invoice.")
                )

                when (val params = toolCall.parameters) {
                    is ToolParameters.SearchClients -> AgentOutcome.ToolExecutionRequested(
                        toolCallId = ToolCallId(toolCall.id),
                        toolName = ToolName.SearchClients,
                        arguments = SearchClientsArgs(NaturalLanguage(params.query))
                    )

                    is ToolParameters.CreateDraftInvoice -> {
                        val client = clientRepository.searchClients(params.clientName).firstOrNull()
                            ?: return AgentOutcome.Failure(
                                FailureMessage("I could not find a client named ${params.clientName}.")
                            )
                        AgentOutcome.ToolExecutionRequested(
                            toolCallId = ToolCallId(toolCall.id),
                            toolName = ToolName.CreateDraftInvoice,
                            arguments = CreateDraftInvoiceArgs(client.id)
                        )
                    }

                    is ToolParameters.DeleteInvoice -> AgentOutcome.ToolExecutionRequested(
                        toolCallId = ToolCallId(toolCall.id),
                        toolName = ToolName.DeleteInvoiceForApproval,
                        arguments = DeleteInvoiceApprovalArgs(params.invoiceId)
                    )

                    else -> unsupportedTool(toolCall.type)
                }
            }
        }
    }

    private fun unsupportedTool(toolType: ToolType): AgentOutcome {
        return AgentOutcome.TextResponse(
            NaturalLanguage("I understood the request as ${toolType.name}, but that action is not wired into the typed agent yet.")
        )
    }
}
