package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.repository.ForemanSessionPersistencePort
import com.fordham.toolbelt.domain.repository.PersistedForemanState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForemanSessionStoreTest {
    @Test
    fun `restore loads persisted session and clears in-flight steps`() = runTest {
        val session = ForemanSession.empty(SessionId("persist-1")).append(
            ForemanTurn(
                role = AgentRole.User,
                content = NaturalLanguage("invoice acme"),
                timestamp = TimestampMillis(1L)
            )
        )
        val persistence = RecordingForemanSessionPersistence(
            PersistedForemanState(session, "ctx")
        )
        val store = ForemanSessionStore(persistence, CoroutineScope(UnconfinedTestDispatcher()))
        store.replaceSteps(
            mutableListOf(
                ChainedToolStep(
                    toolCallId = ToolCallId("step-1"),
                    toolName = ToolName.SearchClients,
                    result = ToolExecutionResult.ClientSearchCompleted(emptyList())
                )
            )
        )

        store.ensureRestored()

        assertEquals(session.sessionId, store.session.sessionId)
        assertEquals("ctx", store.lastSystemPrompt)
        assertTrue(store.completedSteps.isEmpty())
    }

    private class RecordingForemanSessionPersistence(
        private val seeded: PersistedForemanState
    ) : ForemanSessionPersistencePort {
        override suspend fun load(): PersistedForemanState = seeded
        override suspend fun save(state: PersistedForemanState) = Unit
        override suspend fun clear() = Unit
    }
}
