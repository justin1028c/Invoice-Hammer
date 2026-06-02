package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.ReceiptId

data class ClientSearchHit(
    val clientId: ClientId,
    val displayName: NaturalLanguage,
    val totalInvoiced: Double = 0.0,
    val totalOwed: Double = 0.0,
    val invoiceCount: Int = 0,
    val address: String = ""
)

data class UnbilledReceiptSummary(
    val receiptId: ReceiptId,
    val supplierName: NaturalLanguage,
    val amount: CurrencyAmountCents
)

sealed interface ToolExecutionResult {
    val toolName: ToolName
    val uiEffects: List<AgentUiEffect>

    data class ClientSearchCompleted(
        val clients: List<ClientSearchHit>,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.SearchClients
    }

    data class ClientSelected(
        val clientId: ClientId,
        val displayName: NaturalLanguage,
        override val uiEffects: List<AgentUiEffect>
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.SelectClient
    }

    data class ClientDetailsLoaded(
        val clientId: ClientId,
        val summary: NaturalLanguage,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.GetClientDetails
    }

    data class UnbilledReceiptsFound(
        val clientId: ClientId,
        val receipts: List<UnbilledReceiptSummary>,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.GetUnbilledReceipts
    }

    data class TabOpened(
        val tab: AppTab,
        override val uiEffects: List<AgentUiEffect>
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.OpenTab
    }

    data class DraftInvoiceCreated(
        val invoiceId: InvoiceId,
        val clientId: ClientId,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.CreateDraftInvoice
    }

    data class DraftInvoiceUpdated(
        val lineItemCount: Int,
        val clientName: NaturalLanguage,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.UpdateDraftInvoice
    }

    data class JobNoteAdded(
        val clientName: NaturalLanguage,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.AddJobNote
    }

    data class InvoiceSavedFromDraft(
        val invoiceId: InvoiceId,
        val pdfPath: String,
        val clientName: NaturalLanguage,
        val totalAmount: Double,
        val clientEmail: String = "",
        val clientPhone: String = "",
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.SaveInvoiceFromDraft
    }

    data class ClientCreated(
        val clientId: ClientId,
        val displayName: NaturalLanguage,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.CreateClient
    }

    data class ReceiptScanned(
        val itemCount: Int,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.ScanLastReceipt
    }

    data class QuickInvoiceCompleted(
        val invoiceId: InvoiceId,
        val clientName: NaturalLanguage,
        val totalAmount: Double,
        val clientEmail: String = "",
        val clientPhone: String = "",
        override val toolName: ToolName = ToolName.QuickInvoice,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult

    data class QuickInvoiceFromUnbilledCompleted(
        val invoiceId: InvoiceId,
        val clientName: NaturalLanguage,
        val receiptCount: Int,
        val totalAmount: Double,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.QuickInvoiceFromUnbilledReceipts
    }

    data class QuickClientAndInvoiceCompleted(
        val invoiceId: InvoiceId,
        val clientName: NaturalLanguage,
        val totalAmount: Double,
        val clientCreated: Boolean,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.QuickClientAndInvoice
    }

    data class DuplicateAndEditCompleted(
        val clientName: NaturalLanguage,
        val lineItemCount: Int,
        override val uiEffects: List<AgentUiEffect>
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.DuplicateAndEdit
    }

    data class QuickClientLookupCompleted(
        val summary: NaturalLanguage,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.QuickClientLookup
    }

    data class InvoiceHistorySearched(
        val query: NaturalLanguage,
        val summary: NaturalLanguage = NaturalLanguage(""),
        override val uiEffects: List<AgentUiEffect>
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.SearchInvoiceHistory
    }

    data class InvoiceSendQueued(
        val invoiceId: InvoiceId,
        val channel: NaturalLanguage,
        override val toolName: ToolName,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult

    data class InvoiceDeletionQueued(
        val invoiceId: InvoiceId,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.DeleteInvoiceForApproval
    }

    data class OpenLastInvoiceCompleted(
        val pdfPath: String,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.OpenLastInvoice
    }

    data class OpenSupplierCompleted(
        val supplierId: String,
        val name: String,
        val packageName: String,
        val webUrl: String,
        override val uiEffects: List<AgentUiEffect>
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.OpenSupplier
    }

    data class Failure(
        override val toolName: ToolName,
        val error: FailureMessage,
        override val uiEffects: List<AgentUiEffect> = emptyList()
    ) : ToolExecutionResult
}
