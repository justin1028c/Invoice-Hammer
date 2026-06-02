package com.fordham.toolbelt.domain.model.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForemanSessionCompressorTest {
    @Test
    fun `compresses stale history but keeps active user and tool turns`() {
        val sessionId = SessionId("compress-1")
        var session = ForemanSession.empty(sessionId)
        repeat(16) { i ->
            session = session.append(
                ForemanTurn(
                    role = AgentRole.User,
                    content = NaturalLanguage("older request $i"),
                    timestamp = TimestampMillis(i.toLong())
                )
            )
            session = session.append(
                ForemanTurn(
                    role = AgentRole.ToolSystem,
                    content = NaturalLanguage("SEARCH_CLIENTS → Found 1 client(s)."),
                    timestamp = TimestampMillis(i.toLong() + 1),
                    toolName = ToolName.SearchClients
                )
            )
        }
        session = session.append(
            ForemanTurn(
                role = AgentRole.User,
                content = NaturalLanguage("invoice acme for labor 100"),
                timestamp = TimestampMillis(99L)
            )
        )

        val compressed = ForemanSessionCompressor.compressForPrompt(session)

        assertTrue(compressed.history.size < session.history.size)
        assertTrue(
            compressed.history.first().content.value.startsWith(ForemanOperatingRules.SUMMARY_MARKER)
        )
        assertEquals(
            "invoice acme for labor 100",
            compressed.history.last { it.role == AgentRole.User }.content.value
        )
    }

    @Test
    fun `summary block is capped for token budget`() {
        val sessionId = SessionId("cap-1")
        var session = ForemanSession.empty(sessionId)
        repeat(20) { i ->
            session = session.append(
                ForemanTurn(
                    role = AgentRole.User,
                    content = NaturalLanguage("older request $i ".repeat(40)),
                    timestamp = TimestampMillis(i.toLong())
                )
            )
            session = session.append(
                ForemanTurn(
                    role = AgentRole.ToolSystem,
                    content = NaturalLanguage("TOOL_RESULT ".repeat(80)),
                    timestamp = TimestampMillis(i.toLong() + 1),
                    toolName = ToolName.SearchClients
                )
            )
        }
        session = session.append(
            ForemanTurn(
                role = AgentRole.User,
                content = NaturalLanguage("final request"),
                timestamp = TimestampMillis(999L)
            )
        )

        val compressed = ForemanSessionCompressor.compressForPrompt(session)
        val summary = compressed.history.first().content.value

        assertTrue(summary.length <= 2000)
    }
}
