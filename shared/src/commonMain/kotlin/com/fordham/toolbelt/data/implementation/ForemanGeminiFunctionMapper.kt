package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.GeminiFunctionCall
import com.fordham.toolbelt.domain.model.ForemanToolCall
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.ToolParameters
import com.fordham.toolbelt.domain.model.ToolType
import com.fordham.toolbelt.domain.model.agent.apiNameToToolType
import com.fordham.toolbelt.util.randomUUID
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

internal object ForemanGeminiFunctionMapper {
    fun map(call: GeminiFunctionCall): ForemanToolCall? {
        val type = apiNameToToolType(call.name) ?: return null
        if (type == ToolType.UNKNOWN) return null

        val p = call.args.mapValues { (_, element) -> element.toParamString() }
        val parameters = when (type) {
            ToolType.SEARCH_CLIENTS -> ToolParameters.SearchClients(p.string("query"))
            ToolType.SELECT_CLIENT -> ToolParameters.SelectClient(p["clientId"], p["clientName"])
            ToolType.GET_CLIENT_DETAILS -> ToolParameters.GetClientDetails(p["clientId"], p["clientName"])
            ToolType.GET_UNBILLED_RECEIPTS -> ToolParameters.GetUnbilledReceipts(p["clientId"], p["clientName"])
            ToolType.OPEN_TAB -> ToolParameters.OpenTab(p.string("tabName"))
            ToolType.CREATE_DRAFT_INVOICE -> ToolParameters.CreateDraftInvoice(p.string("clientName"))
            ToolType.UPDATE_DRAFT_INVOICE -> ToolParameters.UpdateDraftInvoice(
                clientName = p["clientName"],
                clientAddress = p["clientAddress"],
                taxRate = p.double("taxRate"),
                deposit = p.double("deposit"),
                lineItemsJson = p["lineItems"] ?: p["lineItemsJson"] ?: "[]",
                replaceLineItems = p.bool("replaceLineItems")
            )
            ToolType.ADD_JOB_NOTE -> ToolParameters.AddJobNote(
                p.string("clientName"),
                p.string("note")
            )
            ToolType.SAVE_INVOICE -> ToolParameters.SaveInvoice(p.bool("isEstimate"))
            ToolType.SEARCH_INVOICE_HISTORY -> ToolParameters.SearchInvoiceHistory(p.string("query"))
            ToolType.CREATE_CLIENT -> ToolParameters.CreateClient(
                clientName = p.string("clientName"),
                address = p.string("address"),
                phone = p.string("phone"),
                email = p.string("email")
            )
            ToolType.SCAN_LAST_RECEIPT -> ToolParameters.ScanLastReceipt(p.string("clientName"))
            ToolType.QUICK_INVOICE -> ToolParameters.QuickInvoice(
                clientName = p.string("clientName"),
                clientAddress = p.string("clientAddress"),
                lineItemsJson = p["lineItems"] ?: p["lineItemsJson"] ?: "[]",
                jobDescription = p.string("jobDescription"),
                category = p.string("category"),
                totalAmount = p.string("totalAmount"),
                isEstimate = p.bool("isEstimate"),
                createClientIfMissing = p.bool("createClientIfMissing", default = true)
            )
            ToolType.QUICK_CLIENT_AND_INVOICE -> ToolParameters.QuickClientAndInvoice(
                clientName = p.string("clientName"),
                clientAddress = p.string("clientAddress"),
                clientPhone = p.string("clientPhone").ifBlank { p.string("phone") },
                clientEmail = p.string("clientEmail").ifBlank { p.string("email") },
                lineItemsJson = p["lineItems"] ?: p["lineItemsJson"] ?: "[]",
                jobDescription = p.string("jobDescription"),
                category = p.string("category"),
                totalAmount = p.string("totalAmount"),
                isEstimate = p.bool("isEstimate")
            )
            ToolType.QUICK_CLIENT_LOOKUP -> ToolParameters.QuickClientLookup(p.string("query"))
            ToolType.APPEND_DRAFT_LINES -> ToolParameters.AppendDraftLines(
                lineItemsJson = p["lineItems"] ?: p["lineItemsJson"] ?: "[]"
            )
            ToolType.DUPLICATE_LAST_INVOICE -> ToolParameters.DuplicateLastInvoice(p.string("clientName"))
            ToolType.DUPLICATE_AND_EDIT -> ToolParameters.DuplicateAndEdit(
                clientName = p.string("clientName"),
                lineItemsJson = p["lineItems"] ?: p["lineItemsJson"] ?: "[]"
            )
            ToolType.QUICK_INVOICE_FROM_UNBILLED_RECEIPTS -> ToolParameters.QuickInvoiceFromUnbilledReceipts(
                clientName = p.string("clientName"),
                isEstimate = p.bool("isEstimate"),
                createClientIfMissing = p.bool("createClientIfMissing", default = true)
            )
            ToolType.QUICK_SEND_INVOICE -> ToolParameters.QuickSendInvoice(
                invoiceId = p.string("invoiceId"),
                clientName = p.string("clientName"),
                channel = p.string("channel", default = "sms"),
                recipientPhone = p.string("recipientPhone"),
                recipientEmail = p.string("recipientEmail"),
                message = p.string("message", default = "Your invoice is attached."),
                subject = p.string("subject", default = "Invoice from Invoice Hammer"),
                body = p.string("body", default = "Please find your invoice attached.")
            )
            ToolType.SEND_INVOICE_EMAIL -> ToolParameters.SendInvoiceEmail(
                invoiceId = p.string("invoiceId"),
                recipientEmail = p.string("recipientEmail"),
                subject = p.string("subject", default = "Invoice from Invoice Hammer"),
                body = p.string("body", default = "Please find your invoice attached.")
            )
            ToolType.SEND_INVOICE_SMS -> ToolParameters.SendInvoiceSms(
                invoiceId = p.string("invoiceId"),
                recipientPhone = p.string("recipientPhone"),
                message = p.string("message", default = "Your invoice is attached.")
            )
            ToolType.DELETE_INVOICE -> ToolParameters.DeleteInvoice(InvoiceId(p.string("invoiceId")))
            ToolType.OPEN_LAST_INVOICE -> ToolParameters.OpenLastInvoice(p["invoiceId"])
            ToolType.OPEN_SUPPLIER -> ToolParameters.OpenSupplier(p["supplierId"], p["supplierName"])
            else -> ToolParameters.None
        }

        return ForemanToolCall(
            id = randomUUID(),
            type = type,
            parameters = parameters,
            reasoning = ""
        )
    }

    private fun JsonElement.toParamString(): String = when (this) {
        is JsonPrimitive -> {
            if (isString) content
            else booleanOrNull?.toString() ?: doubleOrNull?.toString() ?: content
        }
        else -> toString()
    }

    private fun Map<String, String>.string(key: String, default: String = ""): String =
        this[key]?.takeIf { it.isNotBlank() } ?: default

    private fun Map<String, String>.double(key: String): Double? =
        this[key]?.toDoubleOrNull()

    private fun Map<String, String>.bool(key: String, default: Boolean = false): Boolean {
        if (!containsKey(key)) return default
        return this[key]?.equals("true", ignoreCase = true) == true
    }
}
