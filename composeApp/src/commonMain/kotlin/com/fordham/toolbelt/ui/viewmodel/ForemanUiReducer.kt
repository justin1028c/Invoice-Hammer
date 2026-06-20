package com.fordham.toolbelt.ui.viewmodel

import com.fordham.toolbelt.domain.model.AgentMode
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.ForemanAgentPresentation
import com.fordham.toolbelt.domain.model.agent.ForemanUserMessageMapper
import com.fordham.toolbelt.util.UiMessageKeys

object ForemanUiReducer {
    fun applyOutcome(
        current: AgentUiState,
        outcome: AgentOutcome,
        completedStepCount: Int
    ): AgentUiState = when (outcome) {
        is AgentOutcome.TextResponse -> current.copy(
            isProcessing = false,
            lastResponse = outcome.response.value,
            isActive = true,
            stepSummaries = emptyList(),
            completedStepCount = completedStepCount,
            pendingApproval = null,
            clientChoices = emptyList(),
            savePreview = null,
            pendingSaveApproval = null,
            errorMessage = null
        )
        is AgentOutcome.ToolChainExecuted -> {
            val message = ForemanUserMessageMapper.forChain(outcome.steps, outcome.finalMessage)
            current.copy(
                isProcessing = false,
                lastResponse = message.spoken.value,
                isActive = true,
                completedStepCount = outcome.steps.size,
                stepSummaries = message.stepLabels,
                currentMode = AgentMode.ACTION,
                pendingApproval = null,
                clientChoices = emptyList(),
                savePreview = null,
                pendingSaveApproval = null,
                errorMessage = null
            )
        }
        is AgentOutcome.RequiresApproval -> current.copy(
            isProcessing = false,
            pendingApproval = outcome,
            lastResponse = ForemanAgentPresentation.approvalMessage(outcome),
            isActive = true,
            stepSummaries = emptyList(),
            completedStepCount = completedStepCount,
            clientChoices = emptyList(),
            savePreview = null,
            pendingSaveApproval = null,
            errorMessage = null
        )
        is AgentOutcome.ClientChoiceRequired -> current.copy(
            isProcessing = false,
            clientChoices = outcome.candidates,
            lastResponse = UiMessageKeys.WHICH_CLIENT_MEANT,
            isActive = true,
            stepSummaries = emptyList(),
            completedStepCount = completedStepCount,
            pendingApproval = null,
            savePreview = null,
            pendingSaveApproval = null,
            errorMessage = null
        )
        is AgentOutcome.SaveConfirmationRequired -> current.copy(
            isProcessing = false,
            savePreview = outcome.preview,
            pendingSaveApproval = outcome,
            lastResponse = UiMessageKeys.reviewInvoice(outcome.preview.clientName.value),
            isActive = true,
            stepSummaries = emptyList(),
            completedStepCount = completedStepCount,
            pendingApproval = null,
            clientChoices = emptyList(),
            errorMessage = null
        )
        is AgentOutcome.TabNavigationCompleted -> current.copy(
            isProcessing = false,
            isActive = true,
            lastResponse = outcome.userMessage.value,
            typedCommand = "",
            errorMessage = null,
            pendingApproval = null,
            clientChoices = emptyList(),
            savePreview = null,
            pendingSaveApproval = null,
            stepSummaries = emptyList(),
            completedStepCount = 0
        )
        is AgentOutcome.Failure -> current.copy(
            isProcessing = false,
            errorMessage = outcome.error.value,
            stepSummaries = emptyList()
        )
        is AgentOutcome.ToolExecuted,
        is AgentOutcome.ToolExecutionRequested -> current.copy(
            isProcessing = false,
            errorMessage = UiMessageKeys.UNHANDLED_TOOL_REQUEST,
            isActive = true
        )
    }
}
