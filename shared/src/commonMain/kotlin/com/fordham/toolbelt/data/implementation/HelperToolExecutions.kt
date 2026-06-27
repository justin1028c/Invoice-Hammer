package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.agent.DetectChangeOrdersOutcome
import com.fordham.toolbelt.domain.usecase.agent.DetectChangeOrdersUseCase
import com.fordham.toolbelt.domain.usecase.agent.GetDailyBriefingOutcome
import com.fordham.toolbelt.domain.usecase.agent.GetDailyBriefingUseCase
import com.fordham.toolbelt.domain.usecase.agent.GetProfitGuardianStatusUseCase
import com.fordham.toolbelt.domain.usecase.agent.ProfitGuardianOutcome

class HelperToolExecutions(
    private val getProfitGuardianStatus: GetProfitGuardianStatusUseCase,
    private val detectChangeOrders: DetectChangeOrdersUseCase,
    private val getDailyBriefing: GetDailyBriefingUseCase
) {
    fun executeOpenTab(arguments: OpenTabArgs): ToolExecutionResult {
        return ToolExecutionResult.TabOpened(
            tab = arguments.tab,
            uiEffects = listOf(AgentUiEffect.NavigateToTab(arguments.tab))
        )
    }

    suspend fun executeGetProfitGuardianStatus(arguments: GetProfitGuardianStatusArgs): ToolExecutionResult {
        return when (val outcome = getProfitGuardianStatus(arguments.invoiceId)) {
            is ProfitGuardianOutcome.Success -> ToolExecutionResult.GetProfitGuardianStatusCompleted(outcome.status)
            is ProfitGuardianOutcome.ProjectNotFound -> ToolExecutionResult.Failure(
                ToolName.GetProfitGuardianStatus,
                FailureMessage("Estimate/Invoice project not found: ${arguments.invoiceId.value}")
            )
            is ProfitGuardianOutcome.Error -> ToolExecutionResult.Failure(
                ToolName.GetProfitGuardianStatus,
                outcome.message
            )
        }
    }

    suspend fun executeDetectChangeOrders(arguments: DetectChangeOrdersArgs): ToolExecutionResult {
        return when (val outcome = detectChangeOrders(arguments.invoiceId)) {
            is DetectChangeOrdersOutcome.Success -> ToolExecutionResult.DetectChangeOrdersCompleted(outcome.opportunities)
            is DetectChangeOrdersOutcome.ProjectNotFound -> ToolExecutionResult.Failure(
                ToolName.DetectChangeOrders,
                FailureMessage("Estimate/Invoice project not found: ${arguments.invoiceId.value}")
            )
            is DetectChangeOrdersOutcome.Error -> ToolExecutionResult.Failure(
                ToolName.DetectChangeOrders,
                outcome.message
            )
        }
    }

    suspend fun executeGetDailyBriefing(arguments: GetDailyBriefingArgs): ToolExecutionResult {
        return when (val outcome = getDailyBriefing.execute()) {
            is GetDailyBriefingOutcome.Success -> ToolExecutionResult.GetDailyBriefingCompleted(outcome.briefing)
            is GetDailyBriefingOutcome.Error -> ToolExecutionResult.Failure(
                ToolName.GetDailyBriefing,
                outcome.message
            )
        }
    }
}
