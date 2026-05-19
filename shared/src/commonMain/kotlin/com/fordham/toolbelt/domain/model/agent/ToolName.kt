package com.fordham.toolbelt.domain.model.agent

sealed interface ToolName {
    data object SearchClients : ToolName
    data object GetUnbilledReceipts : ToolName
    data object CreateDraftInvoice : ToolName
    data object SendInvoiceForApproval : ToolName
    data object DeleteInvoiceForApproval : ToolName
}

enum class ToolSafety {
    Safe,
    RequiresApproval
}

object ForemanToolPolicy {
    fun safetyFor(toolName: ToolName): ToolSafety {
        return when (toolName) {
            ToolName.SearchClients,
            ToolName.GetUnbilledReceipts,
            ToolName.CreateDraftInvoice -> ToolSafety.Safe
            ToolName.SendInvoiceForApproval,
            ToolName.DeleteInvoiceForApproval -> ToolSafety.RequiresApproval
        }
    }
}
