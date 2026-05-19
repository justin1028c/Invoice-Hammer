package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.DeleteInvoiceApprovalArgs
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.SearchClientsArgs
import com.fordham.toolbelt.domain.model.agent.SendInvoiceApprovalArgs
import com.fordham.toolbelt.domain.model.agent.SessionId
import com.fordham.toolbelt.domain.model.agent.ToolExecutionResult
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.model.agent.AgentOutcome as TypedAgentOutcome
import com.fordham.toolbelt.domain.repository.ToolRegistry
import com.fordham.toolbelt.domain.usecase.RunForemanAgentUseCase
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AgentUiState(
    val isActive: Boolean = false,
    val isProcessing: Boolean = false,
    val lastResponse: String? = null,
    val transcript: String = "",
    val currentMode: AgentMode = AgentMode.RESPONSE,
    val errorMessage: String? = null,
    val isListening: Boolean = false,
    val pendingApproval: ForemanToolCall? = null,
    val pendingTypedApproval: TypedAgentOutcome.RequiresApproval? = null
)

class AgentViewModel(
    private val runForemanAgentUseCase: RunForemanAgentUseCase,
    private val toolRegistry: ToolRegistry
) : ViewModel() {

    private var session = ForemanSession.empty(SessionId(randomUUID()))
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    fun executeAgentCommand(command: String, appContext: String, onIntent: (AiAgentIntent) -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    transcript = command,
                    errorMessage = null,
                    pendingApproval = null,
                    pendingTypedApproval = null
                )
            }

            val result = runForemanAgentUseCase(
                command = NaturalLanguage(command),
                session = session,
                systemPrompt = NaturalLanguage(appContext)
            )

            when (result) {
                is TypedAgentOutcome.TextResponse -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            lastResponse = result.response.value,
                            isActive = true
                        )
                    }
                }
                is TypedAgentOutcome.ToolExecuted -> {
                    val summary = summarizeToolResult(result.result)
                    dispatchIntentForToolResult(result.result, onIntent)
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            lastResponse = summary,
                            isActive = true
                        )
                    }
                }
                is TypedAgentOutcome.RequiresApproval -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            pendingApproval = result.toLegacyPendingCall(),
                            pendingTypedApproval = result,
                            isActive = true
                        )
                    }
                }
                is TypedAgentOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = result.error.value
                        )
                    }
                }
                is TypedAgentOutcome.ToolExecutionRequested -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "Agent returned an unhandled tool request.",
                            isActive = true
                        )
                    }
                }
            }
        }
    }

    fun approveToolCall() {
        val pending = _uiState.value.pendingTypedApproval ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, pendingApproval = null, pendingTypedApproval = null) }
            val result = toolRegistry.execute(pending.toolName, pending.arguments)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    lastResponse = summarizeToolResult(result),
                    errorMessage = (result as? ToolExecutionResult.Failure)?.error?.value
                )
            }
        }
    }

    fun setAgentActive(active: Boolean) {
        _uiState.update { it.copy(isActive = active) }
    }

    fun setListening(listening: Boolean) {
        _uiState.update { it.copy(isListening = listening) }
    }

    fun clearAgentResponse() {
        _uiState.update {
            it.copy(
                lastResponse = null,
                errorMessage = null,
                isActive = false,
                pendingApproval = null,
                pendingTypedApproval = null
            )
        }
    }

    private fun summarizeToolResult(result: ToolExecutionResult): String {
        return when (result) {
            is ToolExecutionResult.ClientSearchCompleted -> {
                if (result.clients.isEmpty()) {
                    "No matching clients found."
                } else {
                    "Found ${result.clients.size} matching client${if (result.clients.size == 1) "" else "s"}: ${
                        result.clients.take(3).joinToString { it.displayName.value }
                    }"
                }
            }
            is ToolExecutionResult.DraftInvoiceCreated -> "Draft invoice started for the selected client."
            is ToolExecutionResult.UnbilledReceiptsFound -> {
                "Found ${result.receipts.size} unbilled receipt${if (result.receipts.size == 1) "" else "s"}."
            }
            is ToolExecutionResult.InvoiceApprovalQueued -> "Invoice is ready for approval."
            is ToolExecutionResult.InvoiceDeletionQueued -> "Invoice deleted after approval."
            is ToolExecutionResult.Failure -> result.error.value
        }
    }

    private fun dispatchIntentForToolResult(
        result: ToolExecutionResult,
        onIntent: (AiAgentIntent) -> Unit
    ) {
        when (result) {
            is ToolExecutionResult.DraftInvoiceCreated -> onIntent(AiAgentIntent.DraftInvoice(null))
            is ToolExecutionResult.ClientSearchCompleted -> {
                result.clients.firstOrNull()?.let { onIntent(AiAgentIntent.SummarizeClient(it.displayName.value)) }
            }
            is ToolExecutionResult.UnbilledReceiptsFound -> onIntent(AiAgentIntent.ScanReceipt)
            else -> Unit
        }
    }

    private fun TypedAgentOutcome.RequiresApproval.toLegacyPendingCall(): ForemanToolCall {
        return ForemanToolCall(
            id = toolCallId.value,
            type = when (toolName) {
                ToolName.DeleteInvoiceForApproval -> ToolType.DELETE_INVOICE
                ToolName.SendInvoiceForApproval -> ToolType.UNKNOWN
                else -> ToolType.UNKNOWN
            },
            parameters = when (val args = arguments) {
                is DeleteInvoiceApprovalArgs -> ToolParameters.DeleteInvoice(args.invoiceId)
                is SendInvoiceApprovalArgs -> ToolParameters.DeleteInvoice(args.invoiceId)
                is SearchClientsArgs -> ToolParameters.SearchClients(args.query.value)
                else -> ToolParameters.None
            },
            reasoning = "This action requires your approval before Invoice Hammer executes it."
        )
    }
}
