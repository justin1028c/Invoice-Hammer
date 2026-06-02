package com.fordham.toolbelt.domain.model

enum class ToolCategory {
    SAFE,
    DESTRUCTIVE
}

enum class ToolType(val category: ToolCategory) {
    SEARCH_CLIENTS(ToolCategory.SAFE),
    SELECT_CLIENT(ToolCategory.SAFE),
    GET_CLIENT_DETAILS(ToolCategory.SAFE),
    GET_UNBILLED_RECEIPTS(ToolCategory.SAFE),
    OPEN_TAB(ToolCategory.SAFE),
    CREATE_DRAFT_INVOICE(ToolCategory.SAFE),
    UPDATE_DRAFT_INVOICE(ToolCategory.SAFE),
    ADD_JOB_NOTE(ToolCategory.SAFE),
    SAVE_INVOICE(ToolCategory.SAFE),
    SEARCH_INVOICE_HISTORY(ToolCategory.SAFE),
    CREATE_CLIENT(ToolCategory.SAFE),
    SCAN_LAST_RECEIPT(ToolCategory.SAFE),
    QUICK_INVOICE(ToolCategory.SAFE),
    QUICK_CLIENT_AND_INVOICE(ToolCategory.SAFE),
    QUICK_CLIENT_LOOKUP(ToolCategory.SAFE),
    APPEND_DRAFT_LINES(ToolCategory.SAFE),
    DUPLICATE_LAST_INVOICE(ToolCategory.SAFE),
    DUPLICATE_AND_EDIT(ToolCategory.SAFE),
    QUICK_INVOICE_FROM_UNBILLED_RECEIPTS(ToolCategory.SAFE),
    QUICK_SEND_INVOICE(ToolCategory.DESTRUCTIVE),
    SEND_INVOICE_EMAIL(ToolCategory.DESTRUCTIVE),
    SEND_INVOICE_SMS(ToolCategory.DESTRUCTIVE),
    DELETE_INVOICE(ToolCategory.DESTRUCTIVE),
    OPEN_LAST_INVOICE(ToolCategory.SAFE),
    OPEN_SUPPLIER(ToolCategory.SAFE),
    UNKNOWN(ToolCategory.SAFE)
}

sealed interface ToolParameters {
    data class SearchClients(val query: String) : ToolParameters
    data class SelectClient(val clientId: String?, val clientName: String?) : ToolParameters
    data class GetClientDetails(val clientId: String?, val clientName: String?) : ToolParameters
    data class GetUnbilledReceipts(val clientId: String?, val clientName: String?) : ToolParameters
    data class CreateDraftInvoice(val clientName: String) : ToolParameters
    data class UpdateDraftInvoice(
        val clientName: String?,
        val clientAddress: String?,
        val taxRate: Double?,
        val deposit: Double?,
        val lineItemsJson: String,
        val replaceLineItems: Boolean
    ) : ToolParameters
    data class AddJobNote(val clientName: String, val note: String) : ToolParameters
    data class SaveInvoice(val isEstimate: Boolean) : ToolParameters
    data class SearchInvoiceHistory(val query: String) : ToolParameters
    data class CreateClient(
        val clientName: String,
        val address: String = "",
        val phone: String = "",
        val email: String = ""
    ) : ToolParameters
    data class ScanLastReceipt(val clientName: String = "") : ToolParameters
    data class QuickInvoice(
        val clientName: String,
        val clientAddress: String = "",
        val lineItemsJson: String = "[]",
        val jobDescription: String = "",
        val category: String = "",
        val totalAmount: String = "",
        val isEstimate: Boolean = false,
        val createClientIfMissing: Boolean = true
    ) : ToolParameters
    data class QuickClientAndInvoice(
        val clientName: String,
        val clientAddress: String = "",
        val clientPhone: String = "",
        val clientEmail: String = "",
        val lineItemsJson: String = "[]",
        val jobDescription: String = "",
        val category: String = "",
        val totalAmount: String = "",
        val isEstimate: Boolean = false
    ) : ToolParameters
    data class QuickClientLookup(val query: String) : ToolParameters
    data class AppendDraftLines(val lineItemsJson: String) : ToolParameters
    data class DuplicateLastInvoice(val clientName: String) : ToolParameters
    data class DuplicateAndEdit(
        val clientName: String,
        val lineItemsJson: String = "[]"
    ) : ToolParameters
    data class QuickInvoiceFromUnbilledReceipts(
        val clientName: String,
        val isEstimate: Boolean = false,
        val createClientIfMissing: Boolean = true
    ) : ToolParameters
    data class QuickSendInvoice(
        val invoiceId: String = "",
        val clientName: String = "",
        val channel: String = "sms",
        val recipientPhone: String = "",
        val recipientEmail: String = "",
        val message: String = "Your invoice is attached.",
        val subject: String = "Invoice from Invoice Hammer",
        val body: String = "Please find your invoice attached."
    ) : ToolParameters
    data class SendInvoiceEmail(
        val invoiceId: String,
        val recipientEmail: String,
        val subject: String,
        val body: String
    ) : ToolParameters
    data class SendInvoiceSms(
        val invoiceId: String,
        val recipientPhone: String,
        val message: String
    ) : ToolParameters
    data class DeleteInvoice(val invoiceId: InvoiceId) : ToolParameters
    data class OpenTab(val tabName: String) : ToolParameters
    data class OpenLastInvoice(val invoiceId: String? = null) : ToolParameters
    data class OpenSupplier(val supplierId: String?, val supplierName: String?) : ToolParameters
    data object None : ToolParameters
}

data class ForemanToolCall(
    val id: String,
    val type: ToolType,
    val parameters: ToolParameters,
    val reasoning: String = ""
)
