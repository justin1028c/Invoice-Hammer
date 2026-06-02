package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.ClientName
import com.fordham.toolbelt.domain.model.EmailAddress
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.ForemanToolCall
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.PhoneNumber
import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.domain.model.ToolParameters
import com.fordham.toolbelt.domain.model.agent.AddJobNoteArgs
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.AppendDraftLinesArgs
import com.fordham.toolbelt.domain.model.agent.AppTab
import com.fordham.toolbelt.domain.model.agent.CreateClientArgs
import com.fordham.toolbelt.domain.model.agent.CreateDraftInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.DeleteInvoiceApprovalArgs
import com.fordham.toolbelt.domain.model.agent.DuplicateAndEditArgs
import com.fordham.toolbelt.domain.model.agent.DuplicateLastInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.QuickClientAndInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.ForemanRuntimeBinding
import com.fordham.toolbelt.domain.model.agent.GetClientDetailsArgs
import com.fordham.toolbelt.domain.model.agent.GetUnbilledReceiptsArgs
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.OpenLastInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.OpenSupplierArgs
import com.fordham.toolbelt.domain.model.agent.OpenTabArgs
import com.fordham.toolbelt.domain.model.agent.QuickClientLookupArgs
import com.fordham.toolbelt.domain.model.agent.QuickInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.QuickInvoiceFromUnbilledReceiptsArgs
import com.fordham.toolbelt.domain.model.agent.QuickSendInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.SaveInvoiceFromDraftArgs
import com.fordham.toolbelt.domain.model.agent.ScanLastReceiptArgs
import com.fordham.toolbelt.domain.model.agent.SearchClientsArgs
import com.fordham.toolbelt.domain.model.agent.SearchInvoiceHistoryArgs
import com.fordham.toolbelt.domain.model.agent.SelectClientArgs
import com.fordham.toolbelt.domain.model.agent.SendInvoiceEmailArgs
import com.fordham.toolbelt.domain.model.agent.SendInvoiceSmsArgs
import com.fordham.toolbelt.domain.model.agent.ToolCallId
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.model.agent.UpdateDraftInvoiceArgs
import com.fordham.toolbelt.domain.repository.ClientRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.model.ClientListOutcome
import com.fordham.toolbelt.util.StringFuzzyMatcher
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.first

class ForemanToolCallMapper(
    private val clientRepository: ClientRepository,
    private val invoiceRepository: InvoiceRepository
) {
    suspend fun toAgentOutcome(toolCall: ForemanToolCall): AgentOutcome {
        val id = ToolCallId(toolCall.id.ifBlank { randomUUID() })
        return when (val params = toolCall.parameters) {
            is ToolParameters.SearchClients -> AgentOutcome.ToolExecutionRequested(
                id, ToolName.SearchClients, SearchClientsArgs(NaturalLanguage(params.query))
            )
            is ToolParameters.SelectClient -> AgentOutcome.ToolExecutionRequested(
                id,
                ToolName.SelectClient,
                SelectClientArgs(
                    clientId = params.clientId?.takeIf { it.isNotBlank() }?.let { ClientId(it) },
                    clientName = params.clientName?.takeIf { it.isNotBlank() }?.let { NaturalLanguage(it) }
                )
            )
            is ToolParameters.GetClientDetails -> {
                val clientId = resolveClientId(params.clientId, params.clientName)
                    ?: return AgentOutcome.Failure(FailureMessage("Client not found for details lookup."))
                AgentOutcome.ToolExecutionRequested(id, ToolName.GetClientDetails, GetClientDetailsArgs(clientId))
            }
            is ToolParameters.GetUnbilledReceipts -> {
                val clientId = resolveClientId(params.clientId, params.clientName)
                    ?: return AgentOutcome.Failure(FailureMessage("Client not found for receipt lookup."))
                AgentOutcome.ToolExecutionRequested(id, ToolName.GetUnbilledReceipts, GetUnbilledReceiptsArgs(clientId))
            }
            is ToolParameters.OpenTab -> {
                val tab = AppTab.fromName(params.tabName)
                    ?: return AgentOutcome.Failure(FailureMessage("Unknown tab: ${params.tabName}"))
                AgentOutcome.ToolExecutionRequested(id, ToolName.OpenTab, OpenTabArgs(tab))
            }
            is ToolParameters.CreateClient -> AgentOutcome.ToolExecutionRequested(
                id,
                ToolName.CreateClient,
                CreateClientArgs(
                    clientName = NaturalLanguage(params.clientName),
                    address = NaturalLanguage(params.address),
                    phone = PhoneNumber(params.phone),
                    email = EmailAddress(params.email)
                )
            )
            is ToolParameters.ScanLastReceipt -> AgentOutcome.ToolExecutionRequested(
                id,
                ToolName.ScanLastReceipt,
                ScanLastReceiptArgs(
                    clientName = params.clientName.takeIf { it.isNotBlank() }?.let { ClientName(it) }
                )
            )
            is ToolParameters.QuickInvoice -> {
                val lineItems = ForemanLineItemParser.resolveQuickInvoiceLineItems(
                    lineItemsJson = params.lineItemsJson,
                    jobDescription = params.jobDescription,
                    category = params.category,
                    totalAmount = params.totalAmount.toDoubleOrNull()
                )
                AgentOutcome.ToolExecutionRequested(
                    id,
                    ToolName.QuickInvoice,
                    QuickInvoiceArgs(
                        clientName = NaturalLanguage(params.clientName),
                        clientAddress = NaturalLanguage(params.clientAddress),
                        lineItems = lineItems,
                        isEstimate = params.isEstimate,
                        createClientIfMissing = params.createClientIfMissing
                    )
                )
            }
            is ToolParameters.QuickClientAndInvoice -> {
                val lineItems = ForemanLineItemParser.resolveQuickInvoiceLineItems(
                    lineItemsJson = params.lineItemsJson,
                    jobDescription = params.jobDescription,
                    category = params.category,
                    totalAmount = params.totalAmount.toDoubleOrNull()
                )
                AgentOutcome.ToolExecutionRequested(
                    id,
                    ToolName.QuickClientAndInvoice,
                    QuickClientAndInvoiceArgs(
                        clientName = NaturalLanguage(params.clientName),
                        clientAddress = NaturalLanguage(params.clientAddress),
                        clientPhone = PhoneNumber(params.clientPhone),
                        clientEmail = EmailAddress(params.clientEmail),
                        lineItems = lineItems,
                        isEstimate = params.isEstimate
                    )
                )
            }
            is ToolParameters.QuickClientLookup -> AgentOutcome.ToolExecutionRequested(
                id,
                ToolName.QuickClientLookup,
                QuickClientLookupArgs(NaturalLanguage(params.query))
            )
            is ToolParameters.AppendDraftLines -> {
                val lineItems = ForemanLineItemParser.parse(params.lineItemsJson) ?: emptyList()
                AgentOutcome.ToolExecutionRequested(
                    id,
                    ToolName.AppendDraftLines,
                    AppendDraftLinesArgs(lineItems = lineItems)
                )
            }
            is ToolParameters.DuplicateLastInvoice -> AgentOutcome.ToolExecutionRequested(
                id,
                ToolName.DuplicateLastInvoice,
                DuplicateLastInvoiceArgs(NaturalLanguage(params.clientName))
            )
            is ToolParameters.DuplicateAndEdit -> {
                val lineItems = ForemanLineItemParser.parse(params.lineItemsJson) ?: emptyList()
                AgentOutcome.ToolExecutionRequested(
                    id,
                    ToolName.DuplicateAndEdit,
                    DuplicateAndEditArgs(
                        clientName = NaturalLanguage(params.clientName),
                        additionalLineItems = lineItems
                    )
                )
            }
            is ToolParameters.QuickInvoiceFromUnbilledReceipts -> AgentOutcome.ToolExecutionRequested(
                id,
                ToolName.QuickInvoiceFromUnbilledReceipts,
                QuickInvoiceFromUnbilledReceiptsArgs(
                    clientName = NaturalLanguage(params.clientName),
                    isEstimate = params.isEstimate,
                    createClientIfMissing = params.createClientIfMissing
                )
            )
            is ToolParameters.QuickSendInvoice -> {
                val resolvedId = resolveInvoiceId(params.invoiceId, params.clientName)
                    ?: return AgentOutcome.Failure(
                        FailureMessage("No invoice found to send. Save an invoice or provide invoiceId from COMPLETED STEPS.")
                    )
                AgentOutcome.ToolExecutionRequested(
                    id,
                    ToolName.QuickSendInvoice,
                    QuickSendInvoiceArgs(
                        invoiceId = resolvedId,
                        channel = NaturalLanguage(params.channel),
                        recipientEmail = EmailAddress(params.recipientEmail),
                        recipientPhone = PhoneNumber(params.recipientPhone),
                        subject = NaturalLanguage(params.subject),
                        body = NaturalLanguage(params.body),
                        message = NaturalLanguage(params.message)
                    )
                )
            }
            is ToolParameters.CreateDraftInvoice -> {
                val clientId = resolveClientId(null, params.clientName)
                    ?: return AgentOutcome.Failure(
                        FailureMessage("Client not found: ${params.clientName}. Try SEARCH_CLIENTS or CREATE_CLIENT first.")
                    )
                AgentOutcome.ToolExecutionRequested(id, ToolName.CreateDraftInvoice, CreateDraftInvoiceArgs(clientId))
            }
            is ToolParameters.UpdateDraftInvoice -> {
                val lineItems = ForemanLineItemParser.parse(params.lineItemsJson)
                    ?: return AgentOutcome.Failure(
                        FailureMessage("Could not parse line items from AI response. Please try rephrasing your request.")
                    )
                AgentOutcome.ToolExecutionRequested(
                    id,
                    ToolName.UpdateDraftInvoice,
                    UpdateDraftInvoiceArgs(
                        clientName = params.clientName?.takeIf { it.isNotBlank() }?.let { NaturalLanguage(it) },
                        clientAddress = params.clientAddress?.takeIf { it.isNotBlank() }?.let { NaturalLanguage(it) },
                        taxRate = params.taxRate,
                        deposit = params.deposit,
                        lineItems = lineItems,
                        replaceLineItems = params.replaceLineItems
                    )
                )
            }
            is ToolParameters.AddJobNote -> AgentOutcome.ToolExecutionRequested(
                id,
                ToolName.AddJobNote,
                AddJobNoteArgs(NaturalLanguage(params.clientName), NaturalLanguage(params.note))
            )
            is ToolParameters.SaveInvoice -> AgentOutcome.ToolExecutionRequested(
                id, ToolName.SaveInvoiceFromDraft, SaveInvoiceFromDraftArgs(params.isEstimate)
            )
            is ToolParameters.SearchInvoiceHistory -> AgentOutcome.ToolExecutionRequested(
                id, ToolName.SearchInvoiceHistory, SearchInvoiceHistoryArgs(NaturalLanguage(params.query))
            )
            is ToolParameters.SendInvoiceEmail -> AgentOutcome.ToolExecutionRequested(
                id,
                ToolName.SendInvoiceEmail,
                SendInvoiceEmailArgs(
                    invoiceId = InvoiceId(params.invoiceId),
                    recipientEmail = EmailAddress(params.recipientEmail),
                    subject = NaturalLanguage(params.subject),
                    body = NaturalLanguage(params.body)
                )
            )
            is ToolParameters.SendInvoiceSms -> AgentOutcome.ToolExecutionRequested(
                id,
                ToolName.SendInvoiceSms,
                SendInvoiceSmsArgs(
                    invoiceId = InvoiceId(params.invoiceId),
                    recipientPhone = PhoneNumber(params.recipientPhone),
                    message = NaturalLanguage(params.message)
                )
            )
            is ToolParameters.DeleteInvoice -> AgentOutcome.ToolExecutionRequested(
                id, ToolName.DeleteInvoiceForApproval, DeleteInvoiceApprovalArgs(params.invoiceId)
            )
            is ToolParameters.OpenLastInvoice -> AgentOutcome.ToolExecutionRequested(
                id, ToolName.OpenLastInvoice, OpenLastInvoiceArgs(params.invoiceId?.let { InvoiceId(it) })
            )
            is ToolParameters.OpenSupplier -> AgentOutcome.ToolExecutionRequested(
                id,
                ToolName.OpenSupplier,
                OpenSupplierArgs(
                    supplierId = params.supplierId?.takeIf { it.isNotBlank() }?.let { SupplierId(it) },
                    supplierName = params.supplierName?.takeIf { it.isNotBlank() }?.let { NaturalLanguage(it) }
                )
            )
            else -> AgentOutcome.TextResponse(
                NaturalLanguage("I understood ${toolCall.type.name} but that action is not wired yet.")
            )
        }
    }

    private suspend fun resolveClientId(clientId: String?, clientName: String?): ClientId? {
        clientId?.takeIf { it.isNotBlank() }?.let { return ClientId(it) }
        
        // If a name was explicitly dictated, try to match it
        if (!clientName.isNullOrBlank()) {
            val match = findClientMatch(clientName)
            if (match != null) {
                return match
            }
            // If they explicitly named a client, but we couldn't match them, 
            // do NOT fall back to screen selection. Return null so the system 
            // knows this is a brand-new client name!
            return null
        }
        
        // Only fall back to UI selection if NO client name was dictated at all
        val runtime = ForemanRuntimeBinding.current()
        runtime.selectedClientId?.let { return it }
        runtime.selectedClientName?.takeIf { it.isNotBlank() }?.let { name ->
            val match = findClientMatch(name)
            if (match != null) return match
        }
        return null
    }

    private suspend fun findClientMatch(name: String): ClientId? {
        // 1. Direct database LIKE match search
        clientRepository.searchClients(name).firstOrNull()?.id?.let { return it }

        // 2. Fuzzy matching fallback against existing client catalog
        val allClientsOutcome = clientRepository.getAllClients().first()
        if (allClientsOutcome is ClientListOutcome.Success) {
            var bestMatch: com.fordham.toolbelt.domain.model.Client? = null
            var bestScore = 0.0
            for (client in allClientsOutcome.clients) {
                val score = StringFuzzyMatcher.similarity(name, client.name)
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = client
                }
            }
            // 80% similarity threshold
            if (bestScore >= 0.8 && bestMatch != null) {
                return bestMatch.id
            }
        }
        return null
    }

    private suspend fun resolveInvoiceId(invoiceId: String, clientName: String): InvoiceId? {
        invoiceId.takeIf { it.isNotBlank() }?.let { return InvoiceId(it) }
        val runtime = ForemanRuntimeBinding.current()
        runtime.lastSavedInvoiceId?.let { return it }
        val query = clientName.takeIf { it.isNotBlank() }
            ?: runtime.lastSavedInvoiceClientName.orEmpty()
        if (query.isNotBlank()) {
            return invoiceRepository.searchInvoices(query).firstOrNull()?.id
        }
        return invoiceRepository.searchInvoices("").firstOrNull()?.id
    }
}
