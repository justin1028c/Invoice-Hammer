package com.fordham.toolbelt.domain.model

enum class ToolCategory {
    SAFE,       // Read-only or low-impact creation (e.g. search, open tab, draft)
    DESTRUCTIVE // Danger: deleting, clearing (always needs confirmation)
}

enum class ToolType(val category: ToolCategory) {
    SEARCH_CLIENTS(ToolCategory.SAFE),
    GET_CLIENT_DETAILS(ToolCategory.SAFE),
    CREATE_DRAFT_INVOICE(ToolCategory.SAFE),
    DELETE_INVOICE(ToolCategory.DESTRUCTIVE),
    ADD_JOB_NOTE(ToolCategory.SAFE),
    OPEN_TAB(ToolCategory.SAFE),
    SCAN_RECEIPT(ToolCategory.SAFE),
    SHOW_STATS(ToolCategory.SAFE),
    SYNC_CLOUD(ToolCategory.SAFE),
    UNKNOWN(ToolCategory.SAFE)
}

sealed interface ToolParameters {
    data class SearchClients(val query: String) : ToolParameters
    data class GetClientDetails(val clientName: String) : ToolParameters
    data class CreateDraftInvoice(val clientName: String, val amount: Double, val items: String) : ToolParameters
    data class DeleteInvoice(val invoiceId: InvoiceId) : ToolParameters
    data class AddJobNote(val clientName: String, val note: String) : ToolParameters
    data class OpenTab(val tabName: String) : ToolParameters
    data object None : ToolParameters
}

data class ForemanToolCall(
    val id: String,
    val type: ToolType,
    val parameters: ToolParameters,
    val reasoning: String = ""
)

sealed interface OrchestrationResult {
    data class Executed(val summary: String, val toolCall: ForemanToolCall) : OrchestrationResult
    data class ApprovalRequired(val pendingCall: ForemanToolCall) : OrchestrationResult
    data class Failure(val error: FailureMessage) : OrchestrationResult
    data class ResponseOnly(val text: String) : OrchestrationResult
}
