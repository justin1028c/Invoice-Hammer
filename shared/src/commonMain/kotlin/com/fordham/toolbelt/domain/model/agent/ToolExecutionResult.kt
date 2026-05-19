package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.ReceiptId

data class ClientSearchHit(
    val clientId: ClientId,
    val displayName: NaturalLanguage
)

data class UnbilledReceiptSummary(
    val receiptId: ReceiptId,
    val supplierName: NaturalLanguage,
    val amount: CurrencyAmountCents
)

sealed interface ToolExecutionResult {
    val toolName: ToolName

    data class ClientSearchCompleted(
        val clients: List<ClientSearchHit>
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.SearchClients
    }

    data class UnbilledReceiptsFound(
        val clientId: ClientId,
        val receipts: List<UnbilledReceiptSummary>
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.GetUnbilledReceipts
    }

    data class DraftInvoiceCreated(
        val invoiceId: InvoiceId,
        val clientId: ClientId
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.CreateDraftInvoice
    }

    data class InvoiceApprovalQueued(
        val invoiceId: InvoiceId
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.SendInvoiceForApproval
    }

    data class InvoiceDeletionQueued(
        val invoiceId: InvoiceId
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.DeleteInvoiceForApproval
    }

    data class Failure(
        override val toolName: ToolName,
        val error: FailureMessage
    ) : ToolExecutionResult
}
