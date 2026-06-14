package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.EmailAddress
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.AgentRole
import com.fordham.toolbelt.domain.model.agent.ClientSearchHit
import com.fordham.toolbelt.domain.model.agent.CreateDraftInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.OpenTabArgs
import com.fordham.toolbelt.domain.model.agent.AppTab
import com.fordham.toolbelt.domain.model.agent.ResolvedClient
import com.fordham.toolbelt.domain.model.agent.SearchClientsArgs
import com.fordham.toolbelt.domain.model.agent.SessionId
import com.fordham.toolbelt.domain.model.agent.TimestampMillis
import com.fordham.toolbelt.domain.model.agent.ToolArguments
import com.fordham.toolbelt.domain.model.agent.ToolCallId
import com.fordham.toolbelt.domain.model.agent.ToolExecutionResult
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.model.subscription.PremiumFeature
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.model.subscription.TokenConsumptionOutcome
import com.fordham.toolbelt.domain.model.subscription.TokenCount
import com.fordham.toolbelt.domain.repository.AgentLlmGateway
import com.fordham.toolbelt.domain.repository.ForemanAgentDispatchers
import com.fordham.toolbelt.domain.repository.DraftRepository
import com.fordham.toolbelt.domain.repository.SubscriptionRepository
import com.fordham.toolbelt.domain.repository.BillingRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.repository.ToolRegistry
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.DraftInvoice
import kotlinx.coroutines.flow.flowOf
import com.fordham.toolbelt.domain.usecase.subscription.ConsumeTokenUseCase
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase
import com.fordham.toolbelt.util.PlatformActions
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
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
    private val subscriptionRepository = mockk<SubscriptionRepository>(relaxed = true)
    private val billingRepository = mockk<BillingRepository>(relaxed = true)
    private val draftRepository = mockk<DraftRepository>(relaxed = true)
    private val hasSubscriptionFeature = HasSubscriptionFeatureUseCase(subscriptionRepository)
    private val consumeToken = ConsumeTokenUseCase(billingRepository)
    private val platformActions = mockk<PlatformActions>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    init {
        coEvery { draftRepository.getDraft() } returns flowOf(DraftInvoice())
        coEvery { settingsRepository.getBusinessSettings() } returns BusinessSettings()
    }

    private fun foremanUseCase(
        llmGateway: AgentLlmGateway,
        registry: ToolRegistry,
        foremanEntitled: Boolean = true,
        tokenOutcome: TokenConsumptionOutcome = TokenConsumptionOutcome.Success(TokenCount(5))
    ): RunForemanAgentUseCase {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.ForemanAgent) } returns foremanEntitled
        coEvery { billingRepository.consumeToken(PremiumFeature.FOREMAN_AGENT) } returns tokenOutcome
        return RunForemanAgentUseCase(
            llmGateway,
            registry,
            draftRepository,
            dispatchers,
            hasSubscriptionFeature,
            consumeToken,
            platformActions,
            settingsRepository
        )
    }

    @Test
    fun `safe tool request executes through registry then completes chain`() = runTest(testDispatcher) {
        val toolCallId = ToolCallId("tool-1")
        val arguments = SearchClientsArgs(NaturalLanguage("Acme"))
        val llmGateway = FakeAgentLlmGateway(
            AgentOutcome.ToolExecutionRequested(
                toolCallId = toolCallId,
                toolName = ToolName.SearchClients,
                arguments = arguments
            ),
            AgentOutcome.TextResponse(NaturalLanguage("Found them."))
        )
        val registry = FakeToolRegistry(
            result = ToolExecutionResult.ClientSearchCompleted(
                clients = listOf(
                    ClientSearchHit(ClientId("client-1"), NaturalLanguage("Acme Roofing"))
                )
            )
        )
        val useCase = foremanUseCase(llmGateway, registry)

        val run = useCase(
            command = NaturalLanguage("Find Acme"),
            session = ForemanSession.empty(SessionId("session-1")),
            systemPrompt = NaturalLanguage("You are Foreman."),
            timestamp = TimestampMillis(123L)
        )

        assertTrue(run.outcome is AgentOutcome.ToolChainExecuted)
        val chain = run.outcome as AgentOutcome.ToolChainExecuted
        assertEquals(1, chain.steps.size)
        assertEquals(toolCallId, chain.steps.first().toolCallId)
        assertEquals("Found them.", chain.finalMessage.value)
        assertEquals(1, registry.executeCount)
        assertEquals(ToolName.SearchClients, registry.executedToolName)
    }

    @Test
    fun `local macro executes without LLM`() = runTest(testDispatcher) {
        val llmGateway = FakeAgentLlmGateway(
            AgentOutcome.TextResponse(NaturalLanguage("should not run"))
        )
        val registry = FakeToolRegistry(
            result = ToolExecutionResult.QuickInvoiceCompleted(
                invoiceId = InvoiceId("inv-1"),
                clientName = NaturalLanguage("Bob"),
                totalAmount = 200.0
            )
        )
        val useCase = foremanUseCase(llmGateway, registry)

        val run = useCase(
            command = NaturalLanguage("invoice Bob 200 for labor"),
            session = ForemanSession.empty(SessionId("macro-local")),
            systemPrompt = NaturalLanguage("ctx"),
            timestamp = TimestampMillis(1L)
        )

        val chain = run.outcome as AgentOutcome.ToolChainExecuted
        assertEquals(1, chain.steps.size)
        assertEquals(1, registry.executeCount)
        assertEquals(0, llmGateway.promptCount)
        assertEquals(ToolName.QuickInvoice, registry.executedToolName)
    }

    @Test
    fun `navigation only uses local tab routing without LLM`() = runTest(testDispatcher) {
        val llmGateway = FakeAgentLlmGateway(
            AgentOutcome.TextResponse(NaturalLanguage("should not run"))
        )
        val registry = FakeToolRegistry(
            result = ToolExecutionResult.TabOpened(
                tab = AppTab.NewInvoice,
                uiEffects = emptyList()
            )
        )
        val useCase = foremanUseCase(llmGateway, registry)

        val run = useCase(
            command = NaturalLanguage("open new tab"),
            session = ForemanSession.empty(SessionId("nav-local")),
            systemPrompt = NaturalLanguage("ctx"),
            timestamp = TimestampMillis(1L)
        )

        val chain = run.outcome as AgentOutcome.ToolChainExecuted
        assertEquals(1, chain.steps.size)
        assertEquals(1, registry.executeCount)
        assertEquals(0, llmGateway.promptCount)
        assertEquals("Opened NEW.", chain.finalMessage.value)
    }

    @Test
    fun `chains multiple safe tools before completion`() = runTest(testDispatcher) {
        val llmGateway = FakeAgentLlmGateway(
            AgentOutcome.ToolExecutionRequested(
                ToolCallId("t1"),
                ToolName.OpenTab,
                OpenTabArgs(AppTab.NewInvoice)
            ),
            AgentOutcome.ToolExecutionRequested(
                ToolCallId("t2"),
                ToolName.SearchClients,
                SearchClientsArgs(NaturalLanguage("Acme"))
            ),
            AgentOutcome.TextResponse(NaturalLanguage("All set."))
        )
        val registry = FakeToolRegistry(
            result = ToolExecutionResult.TabOpened(
                tab = AppTab.NewInvoice,
                uiEffects = emptyList()
            )
        )
        val useCase = foremanUseCase(llmGateway, registry)

        val run = useCase(
            command = NaturalLanguage("Open invoice tab and find Acme"),
            session = ForemanSession.empty(SessionId("session-chain")),
            systemPrompt = NaturalLanguage("ctx"),
            timestamp = TimestampMillis(1L)
        )

        val chain = run.outcome as AgentOutcome.ToolChainExecuted
        assertEquals(2, chain.steps.size)
        assertEquals(2, registry.executeCount)
        assertEquals("All set.", chain.finalMessage.value)
        assertTrue(run.session.history.any { it.role == AgentRole.ToolSystem })
    }

    @Test
    fun `approval-required tool is not executed automatically`() = runTest(testDispatcher) {
        val arguments = com.fordham.toolbelt.domain.model.agent.SendInvoiceEmailArgs(
            invoiceId = InvoiceId("invoice-1"),
            recipientEmail = EmailAddress("client@example.com"),
            subject = NaturalLanguage("Invoice"),
            body = NaturalLanguage("Please find attached.")
        )
        val llmGateway = FakeAgentLlmGateway(
            AgentOutcome.ToolExecutionRequested(
                toolCallId = ToolCallId("tool-2"),
                toolName = ToolName.SendInvoiceEmail,
                arguments = arguments
            )
        )
        val registry = FakeToolRegistry(
            result = ToolExecutionResult.InvoiceSendQueued(
                invoiceId = InvoiceId("invoice-1"),
                channel = NaturalLanguage("email"),
                toolName = ToolName.SendInvoiceEmail
            )
        )
        val useCase = foremanUseCase(llmGateway, registry)

        val run = useCase(
            command = NaturalLanguage("Send it"),
            session = ForemanSession.empty(SessionId("session-2")),
            systemPrompt = NaturalLanguage("You are Foreman."),
            timestamp = TimestampMillis(456L)
        )

        assertTrue(run.outcome is AgentOutcome.RequiresApproval)
        assertEquals(0, registry.executeCount)
        assertSame(arguments, (run.outcome as AgentOutcome.RequiresApproval).arguments)
    }

    @Test
    fun `approval gate preserves completed safe steps`() = runTest(testDispatcher) {
        val llmGateway = FakeAgentLlmGateway(
            AgentOutcome.ToolExecutionRequested(
                ToolCallId("t1"),
                ToolName.OpenTab,
                OpenTabArgs(AppTab.NewInvoice)
            ),
            AgentOutcome.ToolExecutionRequested(
                ToolCallId("t2"),
                ToolName.SendInvoiceEmail,
                com.fordham.toolbelt.domain.model.agent.SendInvoiceEmailArgs(
                    invoiceId = InvoiceId("inv-1"),
                    recipientEmail = EmailAddress("a@b.com"),
                    subject = NaturalLanguage("Invoice"),
                    body = NaturalLanguage("Hi")
                )
            )
        )
        val registry = FakeToolRegistry(
            result = ToolExecutionResult.TabOpened(AppTab.NewInvoice, emptyList())
        )
        val useCase = foremanUseCase(llmGateway, registry)

        val run = useCase(
            command = NaturalLanguage("Save and email"),
            session = ForemanSession.empty(SessionId("session-2b")),
            systemPrompt = NaturalLanguage("ctx"),
            timestamp = TimestampMillis(1L)
        )

        val approval = run.outcome as AgentOutcome.RequiresApproval
        assertEquals(1, approval.completedSteps.size)
        assertEquals(1, registry.executeCount)
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
        val useCase = foremanUseCase(llmGateway, registry)

        val run = useCase(
            command = NaturalLanguage("Find Acme"),
            session = ForemanSession.empty(SessionId("session-3")),
            systemPrompt = NaturalLanguage("You are Foreman."),
            timestamp = TimestampMillis(789L)
        )

        assertTrue(run.outcome is AgentOutcome.Failure)
        assertEquals(0, registry.executeCount)
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
        val useCase = foremanUseCase(llmGateway, registry)

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
    fun `second failure on same tool stops chain`() = runTest(testDispatcher) {
        val llmGateway = FakeAgentLlmGateway(
            AgentOutcome.ToolExecutionRequested(
                ToolCallId("t-fail-1"),
                ToolName.SearchClients,
                SearchClientsArgs(NaturalLanguage("Ghost Co"))
            ),
            AgentOutcome.ToolExecutionRequested(
                ToolCallId("t-fail-2"),
                ToolName.SearchClients,
                SearchClientsArgs(NaturalLanguage("Ghost Co"))
            )
        )
        val registry = FakeToolRegistry(
            result = ToolExecutionResult.Failure(
                toolName = ToolName.SearchClients,
                error = FailureMessage("No clients matched Ghost Co.")
            )
        )
        val useCase = foremanUseCase(llmGateway, registry)

        val run = useCase(
            command = NaturalLanguage("Invoice Ghost Co"),
            session = ForemanSession.empty(SessionId("session-retry-cap")),
            systemPrompt = NaturalLanguage("ctx"),
            timestamp = TimestampMillis(3L)
        )

        val chain = run.outcome as AgentOutcome.ToolChainExecuted
        assertEquals(2, chain.steps.size)
        assertEquals(2, registry.executeCount)
        assertEquals(2, llmGateway.promptCount)
        assertTrue(chain.finalMessage.value.contains("Tell me what to change"))
    }

    @Test
    fun `tool failure continues chain so LLM can repair`() = runTest(testDispatcher) {
        val llmGateway = FakeAgentLlmGateway(
            AgentOutcome.ToolExecutionRequested(
                ToolCallId("t-fail"),
                ToolName.SearchClients,
                SearchClientsArgs(NaturalLanguage("Ghost Co"))
            ),
            AgentOutcome.TextResponse(NaturalLanguage("Try CREATE_CLIENT instead."))
        )
        val registry = FakeToolRegistry(
            result = ToolExecutionResult.Failure(
                toolName = ToolName.SearchClients,
                error = FailureMessage("No clients matched Ghost Co.")
            )
        )
        val useCase = foremanUseCase(llmGateway, registry)

        val run = useCase(
            command = NaturalLanguage("Invoice Ghost Co"),
            session = ForemanSession.empty(SessionId("session-repair")),
            systemPrompt = NaturalLanguage("ctx"),
            timestamp = TimestampMillis(2L)
        )

        val chain = run.outcome as AgentOutcome.ToolChainExecuted
        assertEquals(1, chain.steps.size)
        assertTrue(chain.steps.first().result is ToolExecutionResult.Failure)
        assertEquals(2, llmGateway.promptCount)
        assertEquals("Try CREATE_CLIENT instead.", chain.finalMessage.value)
        assertEquals(1, registry.executeCount)
    }

    @Test
    fun `blocks run when Foreman not entitled and tokens exhausted`() = runTest(testDispatcher) {
        val llmGateway = FakeAgentLlmGateway(
            AgentOutcome.TextResponse(NaturalLanguage("Should not run."))
        )
        val registry = FakeToolRegistry(
            result = ToolExecutionResult.ClientSearchCompleted(emptyList())
        )
        val useCase = foremanUseCase(
            llmGateway,
            registry,
            foremanEntitled = false,
            tokenOutcome = TokenConsumptionOutcome.InsufficientTokens(PremiumFeature.FOREMAN_AGENT)
        )

        val run = useCase(
            command = NaturalLanguage("Find Acme"),
            session = ForemanSession.empty(SessionId("session-gate")),
            systemPrompt = NaturalLanguage("ctx"),
            timestamp = TimestampMillis(3L)
        )

        assertTrue(run.outcome is AgentOutcome.Failure)
        assertEquals(0, llmGateway.promptCount)
        assertEquals(0, registry.executeCount)
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
        private val outcomes: ArrayDeque<AgentOutcome>
    ) : AgentLlmGateway {
        constructor(vararg outcomes: AgentOutcome) : this(ArrayDeque(outcomes.toList()))

        var promptedSession: ForemanSession? = null
        var promptCount: Int = 0

        override suspend fun prompt(
            systemPrompt: NaturalLanguage,
            session: ForemanSession,
            functions: List<AgentFunction>
        ): AgentOutcome {
            promptedSession = session
            promptCount++
            return outcomes.removeFirstOrNull()
                ?: AgentOutcome.TextResponse(NaturalLanguage("Done."))
        }
    }

    private class FakeToolRegistry(
        private val result: ToolExecutionResult
    ) : ToolRegistry {
        var executedToolName: ToolName? = null
        var executedArguments: ToolArguments? = null
        var executeCount: Int = 0

        override fun availableFunctions(): List<AgentFunction> = emptyList()

        override suspend fun execute(
            toolName: ToolName,
            arguments: ToolArguments
        ): ToolExecutionResult {
            executeCount++
            executedToolName = toolName
            executedArguments = arguments
            return result
        }
    }
}
