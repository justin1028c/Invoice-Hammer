package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.InvoiceId

sealed interface ToolArguments {
    val expectedToolName: ToolName
}

data class SearchClientsArgs(
    val query: NaturalLanguage
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.SearchClients
}

data class GetUnbilledReceiptsArgs(
    val clientId: ClientId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.GetUnbilledReceipts
}

data class CreateDraftInvoiceArgs(
    val clientId: ClientId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.CreateDraftInvoice
}

data class SendInvoiceApprovalArgs(
    val invoiceId: InvoiceId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.SendInvoiceForApproval
}

data class DeleteInvoiceApprovalArgs(
    val invoiceId: InvoiceId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.DeleteInvoiceForApproval
}

object ToolArgumentValidator {
    fun isCompatible(toolName: ToolName, arguments: ToolArguments): Boolean {
        return toolName == arguments.expectedToolName
    }
}
