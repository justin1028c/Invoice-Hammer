package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.AgentRole
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.ForemanTurn
import com.fordham.toolbelt.domain.model.agent.ForemanToolPolicy
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.TimestampMillis
import com.fordham.toolbelt.domain.model.agent.ToolArgumentValidator
import com.fordham.toolbelt.domain.model.agent.ToolArguments
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.model.agent.ToolSafety
import com.fordham.toolbelt.domain.repository.AgentLlmGateway
import com.fordham.toolbelt.domain.repository.ForemanAgentDispatchers
import com.fordham.toolbelt.domain.repository.ToolRegistry
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class RunForemanAgentUseCase(
    private val llmGateway: AgentLlmGateway,
    private val toolRegistry: ToolRegistry,
    private val dispatchers: ForemanAgentDispatchers
) {
    suspend operator fun invoke(
        command: NaturalLanguage,
        session: ForemanSession,
        systemPrompt: NaturalLanguage,
        timestamp: TimestampMillis = TimestampMillis(Clock.System.now().toEpochMilliseconds())
    ): AgentOutcome = withContext(dispatchers.background) {
        // withContext keeps the full run inside structured concurrency and remains cancellable.
        if (command.value.isBlank()) {
            return@withContext AgentOutcome.Failure(FailureMessage("Agent command cannot be blank."))
        }

        val promptedSession = session.append(
            ForemanTurn(
                role = AgentRole.User,
                content = command,
                timestamp = timestamp
            )
        )

        when (val outcome = llmGateway.prompt(systemPrompt, promptedSession, toolRegistry.availableFunctions())) {
            is AgentOutcome.TextResponse -> outcome
            is AgentOutcome.Failure -> outcome
            is AgentOutcome.ToolExecutionRequested -> handleToolRequest(outcome)
            is AgentOutcome.RequiresApproval -> validateApprovalRequest(outcome)
            is AgentOutcome.ToolExecuted -> AgentOutcome.Failure(
                FailureMessage("LLM gateway cannot mark tools as executed.")
            )
        }
    }

    private suspend fun handleToolRequest(
        request: AgentOutcome.ToolExecutionRequested
    ): AgentOutcome {
        val validationFailure = validateToolRequest(
            toolName = request.toolName,
            arguments = request.arguments
        )
        if (validationFailure != null) return validationFailure

        return when (ForemanToolPolicy.safetyFor(request.toolName)) {
            ToolSafety.RequiresApproval -> AgentOutcome.RequiresApproval(
                toolCallId = request.toolCallId,
                toolName = request.toolName,
                arguments = request.arguments
            )
            ToolSafety.Safe -> AgentOutcome.ToolExecuted(
                toolCallId = request.toolCallId,
                result = toolRegistry.execute(request.toolName, request.arguments)
            )
        }
    }

    private fun validateApprovalRequest(
        request: AgentOutcome.RequiresApproval
    ): AgentOutcome {
        val validationFailure = validateToolRequest(
            toolName = request.toolName,
            arguments = request.arguments
        )
        if (validationFailure != null) return validationFailure

        return when (ForemanToolPolicy.safetyFor(request.toolName)) {
            ToolSafety.RequiresApproval -> request
            ToolSafety.Safe -> AgentOutcome.Failure(
                FailureMessage("Tool does not require approval: ${request.toolName}.")
            )
        }
    }

    private fun validateToolRequest(
        toolName: ToolName,
        arguments: ToolArguments
    ): AgentOutcome.Failure? {
        return if (ToolArgumentValidator.isCompatible(toolName, arguments)) {
            null
        } else {
            AgentOutcome.Failure(
                FailureMessage("Tool arguments do not match requested tool.")
            )
        }
    }
}
