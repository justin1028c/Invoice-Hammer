package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.usecase.ForemanOrchestrator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class AgentUiState(
    val isActive: Boolean = false,
    val isProcessing: Boolean = false,
    val lastResponse: String? = null,
    val transcript: String = "",
    val typedCommand: String = "",
    val currentMode: AgentMode = AgentMode.RESPONSE,
    val errorMessage: String? = null,
    val isListening: Boolean = false,
    val pendingApproval: AgentOutcome.RequiresApproval? = null,
    val clientChoices: List<ClientSearchHit> = emptyList(),
    val savePreview: InvoiceSavePreview? = null,
    val pendingSaveApproval: AgentOutcome.SaveConfirmationRequired? = null,
    val stepSummaries: List<String> = emptyList(),
    val completedStepCount: Int = 0
)

class AgentViewModel(
    private val orchestrator: ForemanOrchestrator
) : ViewModel() {

    val session get() = orchestrator.session

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    private var activeCommandJob: Job? = null

    fun executeAgentCommand(
        command: String,
        appContext: ForemanAppContextBundle,
        onIntent: (AiAgentIntent) -> Unit,
        onEffect: (AgentUiEffect) -> Unit
    ) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return

        activeCommandJob?.cancel()
        activeCommandJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    transcript = trimmed,
                    typedCommand = "",
                    errorMessage = null,
                    pendingApproval = null,
                    clientChoices = emptyList(),
                    savePreview = null,
                    pendingSaveApproval = null,
                    stepSummaries = emptyList()
                )
            }
            val run = orchestrator.run(
                command = NaturalLanguage(trimmed),
                systemPrompt = NaturalLanguage(appContext.systemPrompt),
                runtime = appContext.runtime
            )
            deliverRun(run, onIntent, onEffect)
        }
    }

    fun executeTypedCommand(
        appContext: ForemanAppContextBundle,
        onIntent: (AiAgentIntent) -> Unit,
        onEffect: (AgentUiEffect) -> Unit
    ) {
        val text = _uiState.value.typedCommand.trim()
        if (text.isNotBlank()) {
            ForemanRuntimeBinding.clearTransient()
            executeAgentCommand(text, appContext, onIntent, onEffect)
        }
    }

    fun updateTypedCommand(text: String) {
        _uiState.update { it.copy(typedCommand = text) }
    }

    fun approveToolCall(
        appContext: ForemanAppContextBundle,
        onIntent: (AiAgentIntent) -> Unit,
        onEffect: (AgentUiEffect) -> Unit
    ) {
        val pending = _uiState.value.pendingApproval ?: return
        activeCommandJob?.cancel()
        activeCommandJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, pendingApproval = null) }
            val run = orchestrator.continueAfterApproval(pending, appContext.runtime)
            deliverRun(run, onIntent, onEffect)
        }
    }

    fun selectClientAndContinue(
        clientId: ClientId,
        appContext: ForemanAppContextBundle,
        onIntent: (AiAgentIntent) -> Unit,
        onEffect: (AgentUiEffect) -> Unit
    ) {
        activeCommandJob?.cancel()
        activeCommandJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, clientChoices = emptyList()) }
            val run = orchestrator.continueAfterClientPick(clientId, appContext.runtime)
            deliverRun(run, onIntent, onEffect)
        }
    }

    fun confirmSaveAndContinue(
        appContext: ForemanAppContextBundle,
        onIntent: (AiAgentIntent) -> Unit,
        onEffect: (AgentUiEffect) -> Unit
    ) {
        val pending = _uiState.value.pendingSaveApproval ?: return
        activeCommandJob?.cancel()
        activeCommandJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isProcessing = true, savePreview = null, pendingSaveApproval = null)
            }
            val run = orchestrator.continueAfterSaveConfirm(pending, appContext.runtime)
            deliverRun(run, onIntent, onEffect)
        }
    }

    fun dismissClientChoices() {
        _uiState.update { it.copy(clientChoices = emptyList(), isProcessing = false) }
    }

    fun dismissSavePreview() {
        _uiState.update { it.copy(savePreview = null, pendingSaveApproval = null, isProcessing = false) }
    }

    fun setAgentActive(active: Boolean) {
        _uiState.update { it.copy(isActive = active) }
    }

    fun setListening(listening: Boolean) {
        _uiState.update { it.copy(isListening = listening) }
    }

    fun updateTranscript(text: String) {
        _uiState.update { it.copy(transcript = text) }
    }

    fun clearAgentResponse() {
        activeCommandJob?.cancel()
        activeCommandJob = null
        orchestrator.resetSession()
        _uiState.update { AgentUiState() }
    }

    fun resetSession() {
        orchestrator.resetSession()
        _uiState.update { it.copy(lastResponse = null) }
    }

    private fun deliverRun(
        run: ForemanAgentRun,
        onIntent: (AiAgentIntent) -> Unit,
        onEffect: (AgentUiEffect) -> Unit
    ) {
        when (val outcome = run.outcome) {
            is AgentOutcome.TabNavigationCompleted -> {
                onEffect(AgentUiEffect.NavigateToTab(outcome.tab))
            }
            is AgentOutcome.ToolChainExecuted -> {
                outcome.steps.forEach { step -> dispatchStep(step.result, onIntent, onEffect) }
            }
            is AgentOutcome.RequiresApproval -> {
                outcome.completedSteps.forEach { step -> dispatchStep(step.result, onIntent, onEffect) }
            }
            is AgentOutcome.ClientChoiceRequired -> {
                outcome.completedSteps.forEach { step -> dispatchStep(step.result, onIntent, onEffect) }
            }
            else -> Unit
        }

        _uiState.update {
            ForemanUiReducer.applyOutcome(
                current = it,
                outcome = run.outcome,
                completedStepCount = orchestrator.completedSteps.size
            )
        }
    }

    private fun dispatchStep(
        result: ToolExecutionResult,
        onIntent: (AiAgentIntent) -> Unit,
        onEffect: (AgentUiEffect) -> Unit
    ) {
        ForemanLegacyIntentDispatch.dispatch(result, onIntent)
        result.uiEffects.forEach(onEffect)
    }
}
