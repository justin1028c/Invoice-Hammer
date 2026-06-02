package com.fordham.toolbelt.domain.model.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class ForemanToolCallingPolicyTest {
    @Test
    fun `uses ANY until several tool steps complete`() {
        val session = ForemanSession.empty(com.fordham.toolbelt.domain.model.agent.SessionId("s1"))
            .append(ForemanTurn(AgentRole.User, NaturalLanguage("invoice bob"), TimestampMillis(1)))
            .append(
                ForemanTurn(
                    role = AgentRole.ToolSystem,
                    content = NaturalLanguage("SEARCH ok"),
                    timestamp = TimestampMillis(2),
                    toolName = ToolName.SearchClients
                )
            )
        assertEquals(ForemanToolCallingMode.RequireTool, ForemanToolCallingPolicy.forSession(session))
    }

    @Test
    fun `switches to AUTO after four tool steps`() {
        var session = ForemanSession.empty(com.fordham.toolbelt.domain.model.agent.SessionId("s2"))
            .append(ForemanTurn(AgentRole.User, NaturalLanguage("invoice bob"), TimestampMillis(1)))
        repeat(4) { i ->
            session = session.append(
                ForemanTurn(
                    role = AgentRole.ToolSystem,
                    content = NaturalLanguage("step $i"),
                    timestamp = TimestampMillis(10L + i),
                    toolName = ToolName.SearchClients
                )
            )
        }
        assertEquals(ForemanToolCallingMode.AllowCompletionText, ForemanToolCallingPolicy.forSession(session))
    }
}
