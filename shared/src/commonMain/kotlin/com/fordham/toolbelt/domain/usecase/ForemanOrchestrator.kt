package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.ForemanAgentPresentation
import com.fordham.toolbelt.domain.model.agent.ForemanAgentRun
import com.fordham.toolbelt.domain.model.agent.ForemanCommandRouter
import com.fordham.toolbelt.domain.model.agent.ForemanRoute
import com.fordham.toolbelt.domain.model.agent.ForemanRuntimeBinding
import com.fordham.toolbelt.domain.model.agent.ForemanRuntimeSnapshot
import com.fordham.toolbelt.domain.model.agent.ForemanSessionStore
import com.fordham.toolbelt.domain.model.agent.ForemanToolResultSummarizer
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage

/**
 * Single Foreman entry point — routes local tab/macro vs LLM chain engine.
 */
class ForemanOrchestrator(
    private val chainEngine: RunForemanAgentUseCase,
    private val sessionStore: ForemanSessionStore
) {
    val session get() = sessionStore.session
    val completedSteps get() = sessionStore.completedSteps

    suspend fun run(
        command: NaturalLanguage,
        systemPrompt: NaturalLanguage,
        runtime: ForemanRuntimeSnapshot
    ): ForemanAgentRun {
        sessionStore.ensureRestored()
        ForemanRuntimeBinding.bind(runtime)
        sessionStore.setSystemPrompt(systemPrompt.value)
        sessionStore.clearSteps()

        return when (val route = ForemanCommandRouter.route(command.value)) {
            is ForemanRoute.LocalTab -> ForemanAgentRun(
                outcome = AgentOutcome.TabNavigationCompleted(
                    tab = route.tab,
                    userMessage = ForemanToolResultSummarizer.tabOpenedUserMessage(route.tab)
                ),
                session = sessionStore.session
            )
            is ForemanRoute.LlmChain -> applyRun(
                chainEngine(
                    command = route.command,
                    session = sessionStore.session,
                    systemPrompt = systemPrompt
                )
            )
            is ForemanRoute.LocalMacro -> applyRun(
                chainEngine(
                    command = command,
                    session = sessionStore.session,
                    systemPrompt = systemPrompt
                )
            )
        }
    }

    suspend fun continueAfterApproval(
        pending: AgentOutcome.RequiresApproval,
        runtime: ForemanRuntimeSnapshot
    ): ForemanAgentRun {
        sessionStore.ensureRestored()
        ForemanRuntimeBinding.bind(runtime)
        return applyRun(
            chainEngine.continueAfterApproval(
                pending = pending,
                session = sessionStore.session,
                systemPrompt = NaturalLanguage(sessionStore.lastSystemPrompt),
                completedSteps = sessionStore.completedSteps.toList()
            )
        )
    }

    suspend fun continueAfterClientPick(
        clientId: ClientId,
        runtime: ForemanRuntimeSnapshot
    ): ForemanAgentRun {
        sessionStore.ensureRestored()
        ForemanRuntimeBinding.bind(runtime)
        return applyRun(
            chainEngine.continueAfterClientPick(
                clientId = clientId,
                session = sessionStore.session,
                systemPrompt = NaturalLanguage(sessionStore.lastSystemPrompt),
                completedSteps = sessionStore.completedSteps.toList()
            )
        )
    }

    suspend fun continueAfterSaveConfirm(
        pending: AgentOutcome.SaveConfirmationRequired,
        runtime: ForemanRuntimeSnapshot
    ): ForemanAgentRun {
        sessionStore.ensureRestored()
        ForemanRuntimeBinding.bind(runtime)
        return applyRun(
            chainEngine.continueAfterSaveConfirm(
                pending = pending,
                session = sessionStore.session,
                systemPrompt = NaturalLanguage(sessionStore.lastSystemPrompt),
                completedSteps = sessionStore.completedSteps.toList()
            )
        )
    }

    fun resetSession() {
        sessionStore.reset()
        ForemanRuntimeBinding.reset()
    }

    private fun applyRun(run: ForemanAgentRun): ForemanAgentRun {
        sessionStore.updateSession(run.session)
        sessionStore.replaceSteps(ForemanAgentPresentation.stepsFromOutcome(run.outcome))
        return run
    }
}
