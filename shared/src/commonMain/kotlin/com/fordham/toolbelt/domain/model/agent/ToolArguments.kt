package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.ClientName
import com.fordham.toolbelt.domain.model.EmailAddress
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.PhoneNumber
import com.fordham.toolbelt.domain.model.SupplierId

sealed interface ToolArguments {
    val expectedToolName: ToolName
}

data class SearchClientsArgs(
    val query: NaturalLanguage
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.SearchClients
}

data class SelectClientArgs(
    val clientId: ClientId? = null,
    val clientName: NaturalLanguage? = null
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.SelectClient
}

data class GetClientDetailsArgs(
    val clientId: ClientId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.GetClientDetails
}

data class GetUnbilledReceiptsArgs(
    val clientId: ClientId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.GetUnbilledReceipts
}

data class OpenTabArgs(
    val tab: AppTab
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.OpenTab
}

data class CreateDraftInvoiceArgs(
    val clientId: ClientId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.CreateDraftInvoice
}

data class UpdateDraftInvoiceArgs(
    val clientName: NaturalLanguage? = null,
    val clientAddress: NaturalLanguage? = null,
    val taxRate: Double? = null,
    val deposit: Double? = null,
    val lineItems: List<DraftLineItemInput> = emptyList(),
    val replaceLineItems: Boolean = false
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.UpdateDraftInvoice
}

data class AddJobNoteArgs(
    val clientName: NaturalLanguage,
    val note: NaturalLanguage
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.AddJobNote
}

data class SaveInvoiceFromDraftArgs(
    val isEstimate: Boolean = false,
    val autoShare: Boolean = false
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.SaveInvoiceFromDraft
}

data class SearchInvoiceHistoryArgs(
    val query: NaturalLanguage
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.SearchInvoiceHistory
}

data class CreateClientArgs(
    val clientName: NaturalLanguage,
    val address: NaturalLanguage = NaturalLanguage(""),
    val phone: PhoneNumber = PhoneNumber(""),
    val email: EmailAddress = EmailAddress("")
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.CreateClient
}

data class ScanLastReceiptArgs(
    val clientName: ClientName? = null
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.ScanLastReceipt
}

data class QuickInvoiceArgs(
    val clientName: NaturalLanguage,
    val clientAddress: NaturalLanguage = NaturalLanguage(""),
    val lineItems: List<DraftLineItemInput> = emptyList(),
    val isEstimate: Boolean = false,
    val createClientIfMissing: Boolean = true
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.QuickInvoice
}

data class QuickClientAndInvoiceArgs(
    val clientName: NaturalLanguage,
    val clientAddress: NaturalLanguage = NaturalLanguage(""),
    val clientPhone: PhoneNumber = PhoneNumber(""),
    val clientEmail: EmailAddress = EmailAddress(""),
    val lineItems: List<DraftLineItemInput> = emptyList(),
    val isEstimate: Boolean = false
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.QuickClientAndInvoice
}

data class QuickClientLookupArgs(
    val query: NaturalLanguage
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.QuickClientLookup
}

data class AppendDraftLinesArgs(
    val lineItems: List<DraftLineItemInput>
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.AppendDraftLines
}

data class DuplicateLastInvoiceArgs(
    val clientName: NaturalLanguage
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.DuplicateLastInvoice
}

data class DuplicateAndEditArgs(
    val clientName: NaturalLanguage,
    val additionalLineItems: List<DraftLineItemInput> = emptyList()
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.DuplicateAndEdit
}

data class QuickInvoiceFromUnbilledReceiptsArgs(
    val clientName: NaturalLanguage,
    val isEstimate: Boolean = false,
    val createClientIfMissing: Boolean = true
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.QuickInvoiceFromUnbilledReceipts
}

data class QuickSendInvoiceArgs(
    val invoiceId: InvoiceId,
    val channel: NaturalLanguage,
    val recipientEmail: EmailAddress = EmailAddress(""),
    val recipientPhone: PhoneNumber = PhoneNumber(""),
    val subject: NaturalLanguage = NaturalLanguage("Invoice from Invoice Hammer"),
    val body: NaturalLanguage = NaturalLanguage("Please find your invoice attached."),
    val message: NaturalLanguage = NaturalLanguage("Your invoice is attached.")
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.QuickSendInvoice
}

data class SendInvoiceEmailArgs(
    val invoiceId: InvoiceId,
    val recipientEmail: EmailAddress,
    val subject: NaturalLanguage,
    val body: NaturalLanguage
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.SendInvoiceEmail
}

data class SendInvoiceSmsArgs(
    val invoiceId: InvoiceId,
    val recipientPhone: PhoneNumber,
    val message: NaturalLanguage
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.SendInvoiceSms
}

data class DeleteInvoiceApprovalArgs(
    val invoiceId: InvoiceId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.DeleteInvoiceForApproval
}

data class OpenLastInvoiceArgs(
    val invoiceId: InvoiceId? = null
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.OpenLastInvoice
}

data class OpenSupplierArgs(
    val supplierId: SupplierId? = null,
    val supplierName: NaturalLanguage? = null
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.OpenSupplier
}

data class GetProfitGuardianStatusArgs(
    val invoiceId: InvoiceId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.GetProfitGuardianStatus
}

data class DetectChangeOrdersArgs(
    val invoiceId: InvoiceId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.DetectChangeOrders
}

data class GetDailyBriefingArgs(
    val timestamp: Long = 0L
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.GetDailyBriefing
}

data class CreateChangeOrderArgs(
    val invoiceId: InvoiceId,
    val description: NaturalLanguage,
    val amount: Double
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.CreateChangeOrder
}

object ToolArgumentValidator {
    fun isCompatible(toolName: ToolName, arguments: ToolArguments): Boolean {
        return toolName == arguments.expectedToolName
    }
}
