package com.fordham.toolbelt

import app.cash.turbine.test
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.ClientSearchHit
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.SearchClientsArgs
import com.fordham.toolbelt.domain.model.agent.ToolArguments
import com.fordham.toolbelt.domain.model.agent.ToolCallId
import com.fordham.toolbelt.domain.model.agent.ToolExecutionResult
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.model.agent.AgentOutcome as TypedAgentOutcome
import com.fordham.toolbelt.domain.model.subscription.PremiumFeature
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.model.subscription.TokenConsumptionOutcome
import com.fordham.toolbelt.domain.model.subscription.TokenCount
import com.fordham.toolbelt.domain.repository.AgentLlmGateway
import com.fordham.toolbelt.domain.repository.BillingRepository
import com.fordham.toolbelt.domain.repository.ForemanAgentDispatchers
import com.fordham.toolbelt.domain.repository.DraftRepository
import com.fordham.toolbelt.domain.repository.SubscriptionRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.model.DraftInvoice
import kotlinx.coroutines.flow.flowOf
import com.fordham.toolbelt.domain.repository.ToolRegistry
import com.fordham.toolbelt.domain.usecase.ForemanOrchestrator
import com.fordham.toolbelt.domain.usecase.RunForemanAgentUseCase
import com.fordham.toolbelt.domain.model.agent.ForemanAppContextBundle
import com.fordham.toolbelt.domain.model.agent.ForemanRuntimeSnapshot
import com.fordham.toolbelt.domain.model.agent.ForemanSessionStore
import com.fordham.toolbelt.domain.repository.NoOpForemanSessionPersistencePort
import com.fordham.toolbelt.domain.usecase.subscription.ConsumeTokenUseCase
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase
import com.fordham.toolbelt.util.PlatformActions
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import com.fordham.toolbelt.ui.viewmodel.AgentViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgentViewModelTest {

    private lateinit var viewModel: AgentViewModel
    private lateinit var llmGateway: FakeAgentLlmGateway
    private lateinit var toolRegistry: FakeToolRegistry
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        llmGateway = FakeAgentLlmGateway(
            TypedAgentOutcome.ToolExecutionRequested(
                toolCallId = ToolCallId("tool-1"),
                toolName = ToolName.SearchClients,
                arguments = SearchClientsArgs(NaturalLanguage("Acme"))
            ),
            TypedAgentOutcome.TextResponse(NaturalLanguage("Ready."))
        )
        toolRegistry = FakeToolRegistry(ToolExecutionResult.ClientSearchCompleted(emptyList()))
        viewModel = buildViewModel()
    }

    private fun buildViewModel(): AgentViewModel {
        val subscriptionRepository = mockk<SubscriptionRepository>(relaxed = true)
        val billingRepository = mockk<BillingRepository>(relaxed = true)
        val draftRepository = mockk<DraftRepository>(relaxed = true)
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { subscriptionRepository.hasFeature(SubscriptionFeature.ForemanAgent) } returns true
        coEvery { settingsRepository.getBusinessSettings() } returns BusinessSettings()
        coEvery { billingRepository.consumeToken(PremiumFeature.FOREMAN_AGENT) } returns
            TokenConsumptionOutcome.Success(TokenCount(5))
        coEvery { draftRepository.getDraft() } returns flowOf(DraftInvoice())
        val platformActions = mockk<PlatformActions>(relaxed = true)
        val useCase = RunForemanAgentUseCase(
            llmGateway = llmGateway,
            toolRegistry = toolRegistry,
            draftRepository = draftRepository,
            dispatchers = TestForemanAgentDispatchers(testDispatcher),
            hasSubscriptionFeature = HasSubscriptionFeatureUseCase(subscriptionRepository),
            consumeToken = ConsumeTokenUseCase(billingRepository),
            platformActions = platformActions,
            settingsRepository = settingsRepository
        )
        return AgentViewModel(
            ForemanOrchestrator(
                chainEngine = useCase,
                sessionStore = ForemanSessionStore(
                    NoOpForemanSessionPersistencePort,
                    CoroutineScope(testDispatcher)
                )
            )
        )
    }

    private fun testAppContext(prompt: String = "appContext") =
        ForemanAppContextBundle(prompt, ForemanRuntimeSnapshot.empty())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isActive)
            assertFalse(state.isProcessing)
            assertNull(state.lastResponse)
            assertEquals("", state.transcript)
            assertEquals(AgentMode.RESPONSE, state.currentMode)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `executeAgentCommand sets processing then success state`() = runTest {
        llmGateway = FakeAgentLlmGateway(
            TypedAgentOutcome.ToolExecutionRequested(
            toolCallId = ToolCallId("tool-1"),
            toolName = ToolName.SearchClients,
                arguments = SearchClientsArgs(NaturalLanguage("Acme"))
            ),
            TypedAgentOutcome.TextResponse(NaturalLanguage("Ready."))
        )
        toolRegistry.result = ToolExecutionResult.ClientSearchCompleted(
            listOf(ClientSearchHit(ClientId("client-1"), NaturalLanguage("Acme Roofing")))
        )
        viewModel = buildViewModel()

        var capturedIntent: AiAgentIntent? = null
        viewModel.executeAgentCommand("find Acme", testAppContext(), { intent ->
            capturedIntent = intent
        }, {})

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertTrue(state.isActive)
            assertTrue(state.lastResponse?.contains("Ready.") == true)
            assertNull(state.errorMessage)
            assertTrue(capturedIntent is AiAgentIntent.SummarizeClient)
        }
    }

    @Test
    fun `executeAgentCommand handles error state`() = runTest {
        llmGateway = FakeAgentLlmGateway(
            TypedAgentOutcome.Failure(FailureMessage("Connection failed"))
        )
        viewModel = buildViewModel()

        viewModel.executeAgentCommand("test command", testAppContext(), {}, {})

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage!!.contains("Connection failed"))
        }
    }

    @Test
    fun `setAgentActive updates state`() = runTest {
        viewModel.setAgentActive(true)

        viewModel.uiState.test {
            assertTrue(awaitItem().isActive)
        }
    }

    @Test
    fun `clearAgentResponse clears last response`() = runTest {
        llmGateway = FakeAgentLlmGateway(
            TypedAgentOutcome.TextResponse(NaturalLanguage("Test response"))
        )
        viewModel = buildViewModel()
        viewModel.executeAgentCommand("test", testAppContext(), {}, {})

        viewModel.clearAgentResponse()

        viewModel.uiState.test {
            assertNull(awaitItem().lastResponse)
        }
    }

    private class TestForemanAgentDispatchers(
        override val background: CoroutineDispatcher
    ) : ForemanAgentDispatchers

    private class FakeAgentLlmGateway(
        private val outcomes: ArrayDeque<TypedAgentOutcome>
    ) : AgentLlmGateway {
        constructor(vararg outcomes: TypedAgentOutcome) : this(ArrayDeque(outcomes.toList()))

        override suspend fun prompt(
            systemPrompt: NaturalLanguage,
            session: ForemanSession,
            functions: List<AgentFunction>
        ): TypedAgentOutcome = outcomes.removeFirstOrNull()
            ?: TypedAgentOutcome.TextResponse(NaturalLanguage("Done."))
    }

    private class FakeToolRegistry(
        var result: ToolExecutionResult
    ) : ToolRegistry {
        override fun availableFunctions(): List<AgentFunction> = emptyList()

        override suspend fun execute(
            toolName: ToolName,
            arguments: ToolArguments
        ): ToolExecutionResult = result
    }
}
