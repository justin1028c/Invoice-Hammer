package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.AppTab
import com.fordham.toolbelt.domain.model.agent.ForemanRuntimeSnapshot
import com.fordham.toolbelt.domain.model.agent.ForemanSessionStore
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.repository.NoOpForemanSessionPersistencePort
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForemanOrchestratorTest {
    @Test
    fun `run routes local tab without chain engine`() = runTest {
        val store = ForemanSessionStore(NoOpForemanSessionPersistencePort, this)
        val orchestrator = ForemanOrchestrator(
            chainEngine = mockk(relaxed = true),
            sessionStore = store
        )

        val run = orchestrator.run(
            command = NaturalLanguage("open new invoice tab"),
            systemPrompt = NaturalLanguage("ctx"),
            runtime = ForemanRuntimeSnapshot.empty()
        )

        assertTrue(run.outcome is AgentOutcome.TabNavigationCompleted)
        assertEquals(AppTab.NewInvoice, (run.outcome as AgentOutcome.TabNavigationCompleted).tab)
    }
}
