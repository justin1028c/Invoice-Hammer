package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.repository.*

class CommunicationToolExecutions(
    private val invoiceRepository: InvoiceRepository
) {
    suspend fun executeQuickSendInvoice(arguments: QuickSendInvoiceArgs): ToolExecutionResult {
        val invoice = invoiceRepository.getInvoiceById(arguments.invoiceId)
            ?: return ToolExecutionResult.Failure(ToolName.QuickSendInvoice, FailureMessage("Invoice not found."))
        val channel = arguments.channel.value.lowercase()
        return if (channel.contains("email") || channel.contains("mail")) {
            executeSendInvoiceEmail(
                SendInvoiceEmailArgs(
                    invoiceId = invoice.id,
                    recipientEmail = arguments.recipientEmail.takeIf { it.value.isNotBlank() }
                        ?: invoice.clientEmail,
                    subject = arguments.subject,
                    body = arguments.body
                )
            )
        } else {
            executeSendInvoiceSms(
                SendInvoiceSmsArgs(
                    invoiceId = invoice.id,
                    recipientPhone = arguments.recipientPhone.takeIf { it.value.isNotBlank() }
                        ?: invoice.clientPhone,
                    message = arguments.message
                )
            )
        }
    }

    suspend fun executeSendInvoiceEmail(arguments: SendInvoiceEmailArgs): ToolExecutionResult {
        val invoice = invoiceRepository.getInvoiceById(arguments.invoiceId)
            ?: return ToolExecutionResult.Failure(ToolName.SendInvoiceEmail, FailureMessage("Invoice not found."))
        if (invoice.pdfPath.value.isBlank()) {
            return ToolExecutionResult.Failure(ToolName.SendInvoiceEmail, FailureMessage("Invoice has no PDF yet."))
        }
        return ToolExecutionResult.InvoiceSendQueued(
            invoiceId = invoice.id,
            channel = NaturalLanguage("email"),
            toolName = ToolName.SendInvoiceEmail,
            uiEffects = listOf(
                AgentUiEffect.ShareInvoiceDocument(
                    pdfPath = invoice.pdfPath.value,
                    title = "Invoice",
                    recipientEmail = arguments.recipientEmail.value,
                    subject = arguments.subject.value,
                    body = arguments.body.value
                )
            )
        )
    }

    suspend fun executeSendInvoiceSms(arguments: SendInvoiceSmsArgs): ToolExecutionResult {
        val invoice = invoiceRepository.getInvoiceById(arguments.invoiceId)
            ?: return ToolExecutionResult.Failure(ToolName.SendInvoiceSms, FailureMessage("Invoice not found."))
        if (invoice.pdfPath.value.isBlank()) {
            return ToolExecutionResult.Failure(ToolName.SendInvoiceSms, FailureMessage("Invoice has no PDF yet."))
        }
        return ToolExecutionResult.InvoiceSendQueued(
            invoiceId = invoice.id,
            channel = NaturalLanguage("sms"),
            toolName = ToolName.SendInvoiceSms,
            uiEffects = listOf(
                AgentUiEffect.ShareInvoiceDocument(
                    pdfPath = invoice.pdfPath.value,
                    title = "Invoice",
                    recipientPhone = arguments.recipientPhone.value,
                    body = arguments.message.value
                )
            )
        )
    }
}
