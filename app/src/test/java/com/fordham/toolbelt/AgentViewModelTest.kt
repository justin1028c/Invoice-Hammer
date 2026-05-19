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
import com.fordham.toolbelt.domain.repository.AgentLlmGateway
import com.fordham.toolbelt.domain.repository.ForemanAgentDispatchers
import com.fordham.toolbelt.domain.repository.ToolRegistry
import com.fordham.toolbelt.domain.usecase.RunForemanAgentUseCase
import com.fordham.toolbelt.ui.viewmodel.AgentViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
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
        llmGateway = FakeAgentLlmGateway(TypedAgentOutcome.TextResponse(NaturalLanguage("Ready.")))
        toolRegistry = FakeToolRegistry(ToolExecutionResult.ClientSearchCompleted(emptyList()))
        viewModel = AgentViewModel(
            RunForemanAgentUseCase(
                llmGateway = llmGateway,
                toolRegistry = toolRegistry,
                dispatchers = TestForemanAgentDispatchers(testDispatcher)
            ),
            toolRegistry
        )
    }

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
        llmGateway.outcome = TypedAgentOutcome.ToolExecutionRequested(
            toolCallId = ToolCallId("tool-1"),
            toolName = ToolName.SearchClients,
            arguments = SearchClientsArgs(NaturalLanguage("Acme"))
        )
        toolRegistry.result = ToolExecutionResult.ClientSearchCompleted(
            listOf(ClientSearchHit(ClientId("client-1"), NaturalLanguage("Acme Roofing")))
        )

        var capturedIntent: AiAgentIntent? = null
        viewModel.executeAgentCommand("find Acme", "appContext") { intent ->
            capturedIntent = intent
        }

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertTrue(state.isActive)
            assertEquals("Found 1 matching client: Acme Roofing", state.lastResponse)
            assertNull(state.errorMessage)
            assertTrue(capturedIntent is AiAgentIntent.SummarizeClient)
        }
    }

    @Test
    fun `executeAgentCommand handles error state`() = runTest {
        llmGateway.outcome = TypedAgentOutcome.Failure(FailureMessage("Connection failed"))

        viewModel.executeAgentCommand("test command", "appContext") {}

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
        llmGateway.outcome = TypedAgentOutcome.TextResponse(NaturalLanguage("Test response"))
        viewModel.executeAgentCommand("test", "appContext") {}

        viewModel.clearAgentResponse()

        viewModel.uiState.test {
            assertNull(awaitItem().lastResponse)
        }
    }

    private class TestForemanAgentDispatchers(
        override val background: CoroutineDispatcher
    ) : ForemanAgentDispatchers

    private class FakeAgentLlmGateway(
        var outcome: TypedAgentOutcome
    ) : AgentLlmGateway {
        override suspend fun prompt(
            systemPrompt: NaturalLanguage,
            session: ForemanSession,
            functions: List<AgentFunction>
        ): TypedAgentOutcome = outcome
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
