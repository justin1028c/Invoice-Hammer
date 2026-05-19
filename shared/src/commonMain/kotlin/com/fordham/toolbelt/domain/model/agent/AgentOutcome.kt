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
        val arguments: ToolArguments
    ) : AgentOutcome

    data class ToolExecuted(
        val toolCallId: ToolCallId,
        val result: ToolExecutionResult
    ) : AgentOutcome

    data class Failure(
        val error: FailureMessage
    ) : AgentOutcome
}
