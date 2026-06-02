package com.fordham.toolbelt.domain.model.agent

sealed interface ToolName {
    data object SearchClients : ToolName
    data object SelectClient : ToolName
    data object GetClientDetails : ToolName
    data object GetUnbilledReceipts : ToolName
    data object OpenTab : ToolName
    data object CreateDraftInvoice : ToolName
    data object UpdateDraftInvoice : ToolName
    data object AddJobNote : ToolName
    data object SaveInvoiceFromDraft : ToolName
    data object SearchInvoiceHistory : ToolName
    data object CreateClient : ToolName
    data object ScanLastReceipt : ToolName
    data object QuickInvoice : ToolName
    data object QuickClientAndInvoice : ToolName
    data object QuickClientLookup : ToolName
    data object QuickSendInvoice : ToolName
    data object AppendDraftLines : ToolName
    data object DuplicateLastInvoice : ToolName
    data object DuplicateAndEdit : ToolName
    data object QuickInvoiceFromUnbilledReceipts : ToolName
    data object SendInvoiceEmail : ToolName
    data object SendInvoiceSms : ToolName
    data object DeleteInvoiceForApproval : ToolName
    data object OpenLastInvoice : ToolName
    data object OpenSupplier : ToolName
}

enum class ToolSafety {
    Safe,
    RequiresApproval
}

object ForemanToolPolicy {
    fun safetyFor(toolName: ToolName): ToolSafety {
        return when (toolName) {
            ToolName.SearchClients,
            ToolName.SelectClient,
            ToolName.GetClientDetails,
            ToolName.GetUnbilledReceipts,
            ToolName.OpenTab,
            ToolName.CreateDraftInvoice,
            ToolName.UpdateDraftInvoice,
            ToolName.AddJobNote,
            ToolName.SaveInvoiceFromDraft,
            ToolName.SearchInvoiceHistory,
            ToolName.CreateClient,
            ToolName.ScanLastReceipt,
            ToolName.QuickInvoice,
            ToolName.QuickClientAndInvoice,
            ToolName.QuickClientLookup,
            ToolName.AppendDraftLines,
            ToolName.DuplicateLastInvoice,
            ToolName.DuplicateAndEdit,
            ToolName.QuickInvoiceFromUnbilledReceipts,
            ToolName.OpenLastInvoice,
            ToolName.OpenSupplier -> ToolSafety.Safe
            ToolName.QuickSendInvoice,
            ToolName.SendInvoiceEmail,
            ToolName.SendInvoiceSms,
            ToolName.DeleteInvoiceForApproval -> ToolSafety.RequiresApproval
        }
    }
}
