package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.ClientSearchHit
import com.fordham.toolbelt.domain.model.agent.CreateDraftInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.ResolvedClient
import com.fordham.toolbelt.domain.model.agent.SearchClientsArgs
import com.fordham.toolbelt.domain.model.agent.SessionId
import com.fordham.toolbelt.domain.model.agent.TimestampMillis
import com.fordham.toolbelt.domain.model.agent.ToolArguments
import com.fordham.toolbelt.domain.model.agent.ToolCallId
import com.fordham.toolbelt.domain.model.agent.ToolExecutionResult
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.repository.AgentLlmGateway
import com.fordham.toolbelt.domain.repository.ForemanAgentDispatchers
import com.fordham.toolbelt.domain.repository.ToolRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RunForemanAgentUseCaseTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = TestForemanAgentDispatchers(testDispatcher)

    @Test
    fun `safe tool request executes through registry`() = runTest(testDispatcher) {
        val toolCallId = ToolCallId("tool-1")
        val arguments = SearchClientsArgs(NaturalLanguage("Acme"))
        val llmGateway = FakeAgentLlmGateway(
            AgentOutcome.ToolExecutionRequested(
                toolCallId = toolCallId,
                toolName = ToolName.SearchClients,
                arguments = arguments
            )
        )
        val registry = FakeToolRegistry(
            result = ToolExecutionResult.ClientSearchCompleted(
                clients = listOf(
                    ClientSearchHit(ClientId("client-1"), NaturalLanguage("Acme Roofing"))
                )
            )
        )
        val useCase = RunForemanAgentUseCase(llmGateway, registry, dispatchers)

        val outcome = useCase(
            command = NaturalLanguage("Find Acme"),
            session = ForemanSession.empty(SessionId("session-1")),
            systemPrompt = NaturalLanguage("You are Foreman."),
            timestamp = TimestampMillis(123L)
        )

        assertTrue(outcome is AgentOutcome.ToolExecuted)
        assertEquals(toolCallId, (outcome as AgentOutcome.ToolExecuted).toolCallId)
        assertSame(arguments, registry.executedArguments)
        assertEquals(ToolName.SearchClients, registry.executedToolName)
    }

    @Test
    fun `approval-required tool is not executed automatically`() = runTest(testDispatcher) {
        val arguments = com.fordham.toolbelt.domain.model.agent.SendInvoiceApprovalArgs(
            InvoiceId("invoice-1")
        )
        val llmGateway = FakeAgentLlmGateway(
            AgentOutcome.ToolExecutionRequested(
                toolCallId = ToolCallId("tool-2"),
                toolName = ToolName.SendInvoiceForApproval,
                arguments = arguments
            )
        )
        val registry = FakeToolRegistry(
            result = ToolExecutionResult.InvoiceApprovalQueued(InvoiceId("invoice-1"))
        )
        val useCase = RunForemanAgentUseCase(llmGateway, registry, dispatchers)

        val outcome = useCase(
            command = NaturalLanguage("Send it"),
            session = ForemanSession.empty(SessionId("session-2")),
            systemPrompt = NaturalLanguage("You are Foreman."),
            timestamp = TimestampMillis(456L)
        )

        assertTrue(outcome is AgentOutcome.RequiresApproval)
        assertNull(registry.executedToolName)
        assertSame(arguments, (outcome as AgentOutcome.RequiresApproval).arguments)
    }

    @Test
    fun `tool argument mismatch returns failure`() = runTest(testDispatcher) {
        val llmGateway = FakeAgentLlmGateway(
            AgentOutcome.ToolExecutionRequested(
                toolCallId = ToolCallId("tool-3"),
                toolName = ToolName.SearchClients,
                arguments = CreateDraftInvoiceArgs(ClientId("client-1"))
            )
        )
        val registry = FakeToolRegistry(
            result = ToolExecutionResult.Failure(
                toolName = ToolName.SearchClients,
                error = FailureMessage("Should not execute")
            )
        )
        val useCase = RunForemanAgentUseCase(llmGateway, registry, dispatchers)

        val outcome = useCase(
            command = NaturalLanguage("Find Acme"),
            session = ForemanSession.empty(SessionId("session-3")),
            systemPrompt = NaturalLanguage("You are Foreman."),
            timestamp = TimestampMillis(789L)
        )

        assertTrue(outcome is AgentOutcome.Failure)
        assertNull(registry.executedToolName)
    }

    @Test
    fun `command is appended to prompted session`() = runTest(testDispatcher) {
        val llmGateway = FakeAgentLlmGateway(AgentOutcome.TextResponse(NaturalLanguage("Ready.")))
        val registry = FakeToolRegistry(
            result = ToolExecutionResult.Failure(
                toolName = ToolName.SearchClients,
                error = FailureMessage("Unused")
            )
        )
        val useCase = RunForemanAgentUseCase(llmGateway, registry, dispatchers)

        useCase(
            command = NaturalLanguage("Create a draft invoice"),
            session = ForemanSession.empty(SessionId("session-4")),
            systemPrompt = NaturalLanguage("You are Foreman."),
            timestamp = TimestampMillis(1000L)
        )

        val promptedSession = llmGateway.promptedSession!!
        assertEquals(1, promptedSession.history.size)
        assertEquals("Create a draft invoice", promptedSession.history.first().content.value)
        assertEquals(1000L, promptedSession.history.first().timestamp.value)
    }

    @Test
    fun `resolved entities are immutable and typed`() {
        val original = ForemanSession.empty(SessionId("session-5")).resolvedEntities
        val updated = original.remember(
            alias = NaturalLanguage("Acme"),
            entity = ResolvedClient(ClientId("client-1"))
        )

        assertNotSame(original, updated)
        assertNull(original.resolve(NaturalLanguage("Acme")))
        assertEquals(ResolvedClient(ClientId("client-1")), updated.resolve(NaturalLanguage("Acme")))
    }

    private class TestForemanAgentDispatchers(
        override val background: CoroutineDispatcher
    ) : ForemanAgentDispatchers

    private class FakeAgentLlmGateway(
        private val outcome: AgentOutcome
    ) : AgentLlmGateway {
        var promptedSession: ForemanSession? = null

        override suspend fun prompt(
            systemPrompt: NaturalLanguage,
            session: ForemanSession,
            functions: List<AgentFunction>
        ): AgentOutcome {
            promptedSession = session
            return outcome
        }
    }

    private class FakeToolRegistry(
        private val result: ToolExecutionResult
    ) : ToolRegistry {
        var executedToolName: ToolName? = null
        var executedArguments: ToolArguments? = null

        override fun availableFunctions(): List<AgentFunction> = emptyList()

        override suspend fun execute(
            toolName: ToolName,
            arguments: ToolArguments
        ): ToolExecutionResult {
            executedToolName = toolName
            executedArguments = arguments
            return result
        }
    }
}
