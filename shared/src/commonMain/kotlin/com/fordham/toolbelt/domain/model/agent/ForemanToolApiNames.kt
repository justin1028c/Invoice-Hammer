package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ToolType

/** Stable Gemini function names — must match [ToolType] enum names. */
fun ToolName.toApiName(): String = when (this) {
    ToolName.SearchClients -> ToolType.SEARCH_CLIENTS.name
    ToolName.SelectClient -> ToolType.SELECT_CLIENT.name
    ToolName.GetClientDetails -> ToolType.GET_CLIENT_DETAILS.name
    ToolName.GetUnbilledReceipts -> ToolType.GET_UNBILLED_RECEIPTS.name
    ToolName.OpenTab -> ToolType.OPEN_TAB.name
    ToolName.CreateDraftInvoice -> ToolType.CREATE_DRAFT_INVOICE.name
    ToolName.UpdateDraftInvoice -> ToolType.UPDATE_DRAFT_INVOICE.name
    ToolName.AddJobNote -> ToolType.ADD_JOB_NOTE.name
    ToolName.SaveInvoiceFromDraft -> ToolType.SAVE_INVOICE.name
    ToolName.SearchInvoiceHistory -> ToolType.SEARCH_INVOICE_HISTORY.name
    ToolName.CreateClient -> ToolType.CREATE_CLIENT.name
    ToolName.ScanLastReceipt -> ToolType.SCAN_LAST_RECEIPT.name
    ToolName.QuickInvoice -> ToolType.QUICK_INVOICE.name
    ToolName.QuickClientAndInvoice -> ToolType.QUICK_CLIENT_AND_INVOICE.name
    ToolName.QuickClientLookup -> ToolType.QUICK_CLIENT_LOOKUP.name
    ToolName.AppendDraftLines -> ToolType.APPEND_DRAFT_LINES.name
    ToolName.DuplicateLastInvoice -> ToolType.DUPLICATE_LAST_INVOICE.name
    ToolName.DuplicateAndEdit -> ToolType.DUPLICATE_AND_EDIT.name
    ToolName.QuickInvoiceFromUnbilledReceipts -> ToolType.QUICK_INVOICE_FROM_UNBILLED_RECEIPTS.name
    ToolName.QuickSendInvoice -> ToolType.QUICK_SEND_INVOICE.name
    ToolName.SendInvoiceEmail -> ToolType.SEND_INVOICE_EMAIL.name
    ToolName.SendInvoiceSms -> ToolType.SEND_INVOICE_SMS.name
    ToolName.DeleteInvoiceForApproval -> ToolType.DELETE_INVOICE.name
    ToolName.OpenLastInvoice -> ToolType.OPEN_LAST_INVOICE.name
    ToolName.OpenSupplier -> ToolType.OPEN_SUPPLIER.name
}

fun apiNameToToolType(name: String): ToolType? =
    ToolType.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
