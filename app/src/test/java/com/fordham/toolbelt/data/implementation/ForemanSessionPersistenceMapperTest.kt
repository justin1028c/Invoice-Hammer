package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.agent.AgentRole
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.ForemanTurn
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.ResolvedClient
import com.fordham.toolbelt.domain.model.agent.ResolvedEntities
import com.fordham.toolbelt.domain.model.agent.SessionId
import com.fordham.toolbelt.domain.model.agent.TimestampMillis
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.repository.PersistedForemanState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ForemanSessionPersistenceMapperTest {
    @Test
    fun `round trips session history and resolved entities`() {
        var resolved = ResolvedEntities.empty()
            .remember(NaturalLanguage("acme"), ResolvedClient(ClientId("client-1")))
        val session = ForemanSession(
            sessionId = SessionId("session-42"),
            history = listOf(
                ForemanTurn(
                    role = AgentRole.User,
                    content = NaturalLanguage("invoice acme"),
                    timestamp = TimestampMillis(10L)
                ),
                ForemanTurn(
                    role = AgentRole.ToolSystem,
                    content = NaturalLanguage("SEARCH ok"),
                    timestamp = TimestampMillis(11L),
                    toolName = ToolName.SearchClients
                )
            ),
            activeClient = ClientId("client-1"),
            activeDraftInvoice = null,
            resolvedEntities = resolved
        )
        val state = PersistedForemanState(session, "system prompt")

        val dto = ForemanSessionPersistenceMapper.toDto(state)
        val restored = ForemanSessionPersistenceMapper.fromDto(dto)

        assertNotNull(restored)
        assertEquals(session.sessionId, restored!!.session.sessionId)
        assertEquals(session.history.size, restored.session.history.size)
        assertEquals(ToolName.SearchClients, restored.session.history[1].toolName)
        assertEquals(ClientId("client-1"), restored.session.activeClient)
        assertEquals("system prompt", restored.lastSystemPrompt)
    }
}
