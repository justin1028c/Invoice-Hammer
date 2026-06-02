package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.AiAgentIntent

object ForemanLegacyIntentDispatch {
    fun dispatch(result: ToolExecutionResult, onIntent: (AiAgentIntent) -> Unit) {
        when (result) {
            is ToolExecutionResult.DraftInvoiceCreated,
            is ToolExecutionResult.DraftInvoiceUpdated,
            is ToolExecutionResult.DuplicateAndEditCompleted -> onIntent(AiAgentIntent.DraftInvoice(null))
            is ToolExecutionResult.ClientSearchCompleted -> {
                if (result.clients.size == 1) {
                    onIntent(AiAgentIntent.SummarizeClient(result.clients.first().displayName.value))
                }
            }
            is ToolExecutionResult.ClientSelected -> {
                onIntent(AiAgentIntent.SummarizeClient(result.displayName.value))
            }
            is ToolExecutionResult.UnbilledReceiptsFound -> onIntent(AiAgentIntent.ScanReceipt)
            is ToolExecutionResult.InvoiceHistorySearched -> onIntent(AiAgentIntent.FindJob(result.query.value))
            else -> Unit
        }
    }
}
