package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.FailureMessage

sealed interface AgentOutcome {
    data class TextResponse(
        val response: NaturalLanguage
    ) : AgentOutcome

    data class ToolExecutionRequested(
        val toolCallId: ToolCallId,
        val toolName: ToolName,
        val arguments: ToolArguments
    ) : AgentOutcome

    data class RequiresApproval(
        val toolCallId: ToolCallId,
        val toolName: ToolName,
        val arguments: ToolArguments,
        val completedSteps: List<ChainedToolStep> = emptyList()
    ) : AgentOutcome

    data class ToolExecuted(
        val toolCallId: ToolCallId,
        val result: ToolExecutionResult
    ) : AgentOutcome

    /** One or more safe tools ran in a single agent loop before a final message or approval gate. */
    data class ToolChainExecuted(
        val steps: List<ChainedToolStep>,
        val finalMessage: NaturalLanguage
    ) : AgentOutcome {
        init {
            require(steps.isNotEmpty()) { "ToolChainExecuted requires at least one step." }
        }
    }

    data class Failure(
        val error: FailureMessage
    ) : AgentOutcome

    /** Local tab routing completed without LLM or tool registry (instant nav). */
    data class TabNavigationCompleted(
        val tab: AppTab,
        val userMessage: NaturalLanguage
    ) : AgentOutcome

    /** Multiple client matches — user must pick before the chain continues. */
    data class ClientChoiceRequired(
        val candidates: List<ClientSearchHit>,
        val completedSteps: List<ChainedToolStep> = emptyList()
    ) : AgentOutcome

    /** Draft is ready — user confirms save before PDF is generated. */
    data class SaveConfirmationRequired(
        val preview: InvoiceSavePreview,
        val pendingToolCallId: ToolCallId,
        val pendingArguments: SaveInvoiceFromDraftArgs,
        val completedSteps: List<ChainedToolStep> = emptyList()
    ) : AgentOutcome
}

data class InvoiceSavePreview(
    val clientName: NaturalLanguage,
    val lineItemCount: Int,
    val estimatedTotal: Double,
    val isEstimate: Boolean
)
