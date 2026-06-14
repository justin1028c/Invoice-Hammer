package com.fordham.toolbelt.domain.model.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class ForemanToolCallingPolicyTest {
    @Test
    fun `uses RequireTool when no tool steps completed`() {
        val session = ForemanSession.empty(SessionId("s1"))
            .append(ForemanTurn(AgentRole.User, NaturalLanguage("invoice bob"), TimestampMillis(1)))
        assertEquals(ForemanToolCallingMode.RequireTool, ForemanToolCallingPolicy.forSession(session))
    }

    @Test
    fun `switches to AUTO after one tool step`() {
        val session = ForemanSession.empty(SessionId("s2"))
            .append(ForemanTurn(AgentRole.User, NaturalLanguage("invoice bob"), TimestampMillis(1)))
            .append(
                ForemanTurn(
                    role = AgentRole.ToolSystem,
                    content = NaturalLanguage("SEARCH ok"),
                    timestamp = TimestampMillis(2),
                    toolName = ToolName.SearchClients
                )
            )
        assertEquals(ForemanToolCallingMode.AllowCompletionText, ForemanToolCallingPolicy.forSession(session))
    }
}
