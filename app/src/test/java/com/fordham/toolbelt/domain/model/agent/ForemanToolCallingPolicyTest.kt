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

    @Test
    fun `uses AllowCompletionText for help commands on turn one`() {
        val session = ForemanSession.empty(SessionId("s3"))
            .append(ForemanTurn(AgentRole.User, NaturalLanguage("what can you do"), TimestampMillis(1)))
        assertEquals(ForemanToolCallingMode.AllowCompletionText, ForemanToolCallingPolicy.forSession(session))
    }

    @Test
    fun `uses AllowCompletionText when replying to Foreman conversational turn`() {
        val session = ForemanSession.empty(SessionId("s4"))
            .append(ForemanTurn(AgentRole.User, NaturalLanguage("invoice Bob"), TimestampMillis(1)))
            .append(ForemanTurn(AgentRole.Foreman, NaturalLanguage("What is the address?"), TimestampMillis(2)))
            .append(ForemanTurn(AgentRole.User, NaturalLanguage("ok"), TimestampMillis(3)))
        assertEquals(ForemanToolCallingMode.AllowCompletionText, ForemanToolCallingPolicy.forSession(session))
    }
}
