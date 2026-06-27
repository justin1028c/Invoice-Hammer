package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.GenerateAndSaveInvoiceUseCase
import com.fordham.toolbelt.domain.usecase.GenerateInvoiceRequest
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.first
import kotlin.math.roundToLong

class InvoiceToolExecutions(
    private val clientToolExecutions: ClientToolExecutions,
    private val clientRepository: ClientRepository,
    private val receiptRepository: ReceiptRepository,
    private val draftRepository: DraftRepository,
    private val invoiceRepository: InvoiceRepository,
    private val settingsRepository: SettingsRepository,
    private val generateAndSaveInvoiceUseCase: GenerateAndSaveInvoiceUseCase
) {
    suspend fun executeCreateDraftInvoice(arguments: CreateDraftInvoiceArgs): ToolExecutionResult {
        val client = clientToolExecutions.resolveClient(arguments.clientId)
            ?: return ToolExecutionResult.Failure(ToolName.CreateDraftInvoice, FailureMessage("Client not found."))
        draftRepository.saveDraft(
            DraftInvoice(
                clientName = client.name.value,
                clientAddress = client.address.value,
                taxRate = settingsRepository.getBusinessSettings().taxRate.takeIf { it > 0.0 } ?: 7.0,
                saveToClientDirectory = false
            )
        )
        return ToolExecutionResult.DraftInvoiceCreated(
            invoiceId = InvoiceId("draft-pending"),
            clientId = client.id,
            uiEffects = listOf(
                AgentUiEffect.NavigateToTab(AppTab.NewInvoice),
                AgentUiEffect.SelectClient(client.id)
            )
        )
    }

    suspend fun executeUpdateDraftInvoice(arguments: UpdateDraftInvoiceArgs): ToolExecutionResult {
        val current = draftRepository.getDraft().first()
        val mergedItems = when {
            arguments.replaceLineItems -> arguments.lineItems.map { it.toLineItem() }
            arguments.lineItems.isNotEmpty() -> current.lineItems + arguments.lineItems.map { it.toLineItem() }
            else -> current.lineItems
        }
        val updated = current.copy(
            clientName = arguments.clientName?.value?.takeIf { it.isNotBlank() } ?: current.clientName,
            clientAddress = arguments.clientAddress?.value?.takeIf { it.isNotBlank() } ?: current.clientAddress,
            taxRate = arguments.taxRate ?: current.taxRate,
            deposit = arguments.deposit ?: current.deposit,
            lineItems = mergedItems
        )
        if (updated.clientName.isBlank()) {
            return ToolExecutionResult.Failure(
                ToolName.UpdateDraftInvoice,
                FailureMessage("Draft needs a client name before line items can be added.")
            )
        }
        draftRepository.saveDraft(updated)
        return ToolExecutionResult.DraftInvoiceUpdated(
            lineItemCount = updated.lineItems.size,
            clientName = NaturalLanguage(updated.clientName),
            uiEffects = listOf(AgentUiEffect.NavigateToTab(AppTab.NewInvoice))
        )
    }

    suspend fun executeSaveInvoiceFromDraft(arguments: SaveInvoiceFromDraftArgs): ToolExecutionResult {
        val draft = draftRepository.getDraft().first()
        if (draft.clientName.isBlank() || draft.lineItems.isEmpty()) {
            return ToolExecutionResult.Failure(
                ToolName.SaveInvoiceFromDraft,
                FailureMessage("Draft needs a client and at least one line item before saving.")
            )
        }
        val settings = settingsRepository.getBusinessSettings()
        var savedPath = ""
        val outcome = generateAndSaveInvoiceUseCase(
            GenerateInvoiceRequest(
                clientName = ClientName(draft.clientName),
                clientAddress = ClientAddress(draft.clientAddress),
                saveToClientDirectory = draft.saveToClientDirectory,
                taxRate = TaxRatePercent(draft.taxRate),
                deposit = MoneyAmount(maxOf(0.0, draft.deposit)),
                lineItems = draft.lineItems,
                logoUriString = draft.logoUri?.takeIf { it.isNotBlank() }?.let { MediaUri(it) },
                businessSettings = settings,
                isEstimate = arguments.isEstimate,
                elapsedSeconds = DurationSeconds(draft.elapsedSeconds),
                capturedPhotos = draft.capturedPhotos,
                linkedReceiptIds = draft.linkedReceiptIds.map { ReceiptId(it) },
                availableReceipts = emptyList(),
                onGenerated = { savedPath = it.value }
            )
        )
        return when (outcome) {
            is GenerateInvoiceOutcome.Success -> {
                val subtotal = draft.lineItems.map { it.amount.value }.sum()
                val tax = subtotal * (draft.taxRate / 100.0)
                val savedInvoice = invoiceRepository.getInvoicesByClient(draft.clientName).first()
                    .maxByOrNull { it.lastUpdated }
                val invoiceId = savedInvoice?.id ?: InvoiceId(randomUUID())
                val clientRecord = clientToolExecutions.resolveClientByName(draft.clientName)
                val emailStr = clientRecord?.email?.value.orEmpty()
                val phoneStr = clientRecord?.phone?.value.orEmpty()
                val pdf = savedPath.ifBlank { savedInvoice?.pdfPath?.value.orEmpty() }

                val effects = mutableListOf<AgentUiEffect>(
                    AgentUiEffect.NavigateToTab(AppTab.History)
                )

                if (arguments.autoShare) {
                    effects.add(
                        AgentUiEffect.ShareInvoiceDocument(
                            pdfPath = pdf,
                            title = if (arguments.isEstimate) "Estimate" else "Invoice",
                            recipientEmail = emailStr,
                            recipientPhone = phoneStr,
                            subject = if (arguments.isEstimate) "Estimate from Invoice Hammer" else "Invoice from Invoice Hammer",
                            body = if (arguments.isEstimate) "Please find your estimate attached." else "Please find your invoice attached."
                        )
                    )
                }

                ForemanRuntimeBinding.recordLastSaved(invoiceId, draft.clientName)
                draftRepository.clearDraft()
                ToolExecutionResult.InvoiceSavedFromDraft(
                    invoiceId = invoiceId,
                    pdfPath = pdf,
                    clientName = NaturalLanguage(draft.clientName),
                    totalAmount = subtotal + tax - draft.deposit,
                    clientEmail = emailStr,
                    clientPhone = phoneStr,
                    uiEffects = effects
                )
            }
            is GenerateInvoiceOutcome.Error -> ToolExecutionResult.Failure(
                ToolName.SaveInvoiceFromDraft,
                FailureMessage(outcome.message)
            )
        }
    }

    suspend fun executeSearchInvoiceHistory(arguments: SearchInvoiceHistoryArgs): ToolExecutionResult {
        val query = arguments.query.value
        val matches = invoiceRepository.searchInvoices(query)
        val matchSummary = if (matches.isEmpty()) {
            "No invoices found matching '$query'."
        } else {
            matches.take(5).joinToString("; ") { inv ->
                "id=${inv.id.value}, client=${inv.clientName.value}, total=${com.fordham.toolbelt.util.DateTimeUtil.formatMoney(inv.totalAmount.value)}, date=${inv.date}, paid=${inv.isPaid}"
            }
        }
        return ToolExecutionResult.InvoiceHistorySearched(
            query = arguments.query,
            summary = NaturalLanguage(matchSummary),
            uiEffects = listOf(
                AgentUiEffect.NavigateToTab(AppTab.History),
                AgentUiEffect.SearchHistory(query)
            )
        )
    }

    suspend fun executeQuickInvoice(arguments: QuickInvoiceArgs): ToolExecutionResult {
        var client = clientToolExecutions.resolveClientOrSelected(null, arguments.clientName.value)
        if (client == null && arguments.createClientIfMissing) {
            when (val created = clientToolExecutions.executeCreateClient(
                CreateClientArgs(
                    clientName = arguments.clientName,
                    address = arguments.clientAddress
                )
            )) {
                is ToolExecutionResult.ClientCreated -> client = clientToolExecutions.resolveClient(created.clientId)
                is ToolExecutionResult.Failure -> return created
                else -> Unit
            }
        }
        client ?: return ToolExecutionResult.Failure(
            ToolName.QuickInvoice,
            FailureMessage("Client not found. Say the client name or select a client first.")
        )
        executeCreateDraftInvoice(CreateDraftInvoiceArgs(client.id))
        if (arguments.lineItems.isNotEmpty() || arguments.clientAddress.value.isNotBlank()) {
            val update = executeUpdateDraftInvoice(
                UpdateDraftInvoiceArgs(
                    clientName = NaturalLanguage(client.name.value),
                    clientAddress = arguments.clientAddress.takeIf { it.value.isNotBlank() },
                    lineItems = arguments.lineItems,
                    replaceLineItems = true
                )
            )
            if (update is ToolExecutionResult.Failure) return update
        }
        val currentDraft = draftRepository.getDraft().first()
        return ToolExecutionResult.DraftInvoiceUpdated(
            lineItemCount = currentDraft.lineItems.size,
            clientName = NaturalLanguage(client.name.value),
            uiEffects = listOf(
                AgentUiEffect.NavigateToTab(AppTab.NewInvoice),
                AgentUiEffect.SelectClient(client.id)
            )
        )
    }

    suspend fun executeQuickClientAndInvoice(arguments: QuickClientAndInvoiceArgs): ToolExecutionResult {
        val clientExisted = clientToolExecutions.resolveClientOrSelected(null, arguments.clientName.value) != null
        if (!clientExisted) {
            val created = clientToolExecutions.executeCreateClient(
                CreateClientArgs(
                    clientName = arguments.clientName,
                    address = arguments.clientAddress,
                    phone = arguments.clientPhone,
                    email = arguments.clientEmail
                )
            )
            if (created is ToolExecutionResult.Failure) {
                return created
            }
        } else {
            val existing = clientToolExecutions.resolveClientByName(arguments.clientName.value)
            if (existing != null) {
                val mergedPhone = if (arguments.clientPhone.value.isNotBlank()) arguments.clientPhone else existing.phone
                val mergedEmail = if (arguments.clientEmail.value.isNotBlank()) arguments.clientEmail else existing.email
                val mergedAddress = if (arguments.clientAddress.value.isNotBlank()) ClientAddress(arguments.clientAddress.value) else existing.address
                val updatedClient = existing.copy(
                    address = mergedAddress,
                    phone = mergedPhone,
                    email = mergedEmail,
                    lastUpdated = com.fordham.toolbelt.util.DateTimeUtil.nowEpochMillis()
                )
                clientRepository.insertClient(updatedClient)
            }
        }

        return when (
            val result = executeQuickInvoice(
                QuickInvoiceArgs(
                    clientName = arguments.clientName,
                    clientAddress = arguments.clientAddress,
                    lineItems = arguments.lineItems,
                    isEstimate = arguments.isEstimate,
                    createClientIfMissing = false
                )
            )
        ) {
            is ToolExecutionResult.QuickInvoiceCompleted -> ToolExecutionResult.QuickClientAndInvoiceCompleted(
                invoiceId = result.invoiceId,
                clientName = result.clientName,
                totalAmount = result.totalAmount,
                clientCreated = !clientExisted,
                uiEffects = result.uiEffects
            )
            else -> result
        }
    }

    suspend fun executeDuplicateAndEdit(arguments: DuplicateAndEditArgs): ToolExecutionResult {
        val duplicate = executeDuplicateLastInvoice(DuplicateLastInvoiceArgs(arguments.clientName))
        if (duplicate is ToolExecutionResult.Failure) return duplicate
        if (arguments.additionalLineItems.isEmpty()) {
            val updated = duplicate as ToolExecutionResult.DraftInvoiceUpdated
            return ToolExecutionResult.DuplicateAndEditCompleted(
                clientName = updated.clientName,
                lineItemCount = updated.lineItemCount,
                uiEffects = updated.uiEffects
            )
        }
        return when (val appended = executeAppendDraftLines(AppendDraftLinesArgs(arguments.additionalLineItems))) {
            is ToolExecutionResult.DraftInvoiceUpdated -> ToolExecutionResult.DuplicateAndEditCompleted(
                clientName = appended.clientName,
                lineItemCount = appended.lineItemCount,
                uiEffects = appended.uiEffects
            )
            else -> appended
        }
    }

    suspend fun executeAppendDraftLines(arguments: AppendDraftLinesArgs): ToolExecutionResult {
        if (arguments.lineItems.isEmpty()) {
            return ToolExecutionResult.Failure(
                ToolName.AppendDraftLines,
                FailureMessage("No line items to append.")
            )
        }
        return executeUpdateDraftInvoice(
            UpdateDraftInvoiceArgs(
                lineItems = arguments.lineItems,
                replaceLineItems = false
            )
        )
    }

    suspend fun executeDuplicateLastInvoice(arguments: DuplicateLastInvoiceArgs): ToolExecutionResult {
        val client = clientToolExecutions.resolveClientOrSelected(null, arguments.clientName.value)
            ?: return ToolExecutionResult.Failure(
                ToolName.DuplicateLastInvoice,
                FailureMessage("Client not found: ${arguments.clientName.value}")
            )
        val last = invoiceRepository.getInvoicesByClient(client.name.value).first()
            .maxByOrNull { it.lastUpdated }
            ?: return ToolExecutionResult.Failure(
                ToolName.DuplicateLastInvoice,
                FailureMessage("No prior invoice for ${client.name.value}.")
            )
        executeCreateDraftInvoice(CreateDraftInvoiceArgs(client.id))
        val description = last.itemsSummary.value.takeIf { it.isNotBlank() }
            ?: "Repeat of invoice ${last.date}"
        return executeUpdateDraftInvoice(
            UpdateDraftInvoiceArgs(
                clientName = NaturalLanguage(client.name.value),
                clientAddress = NaturalLanguage(client.address.value),
                lineItems = listOf(
                    DraftLineItemInput(
                        description = NaturalLanguage(description),
                        amount = last.totalAmount.value,
                        category = NaturalLanguage("Service")
                    )
                ),
                replaceLineItems = true
            )
        )
    }

    suspend fun executeQuickInvoiceFromUnbilledReceipts(
        arguments: QuickInvoiceFromUnbilledReceiptsArgs
    ): ToolExecutionResult {
        var client = clientToolExecutions.resolveClientOrSelected(null, arguments.clientName.value)
        if (client == null && arguments.createClientIfMissing) {
            when (val created = clientToolExecutions.executeCreateClient(
                CreateClientArgs(clientName = arguments.clientName)
            )) {
                is ToolExecutionResult.ClientCreated -> client = clientToolExecutions.resolveClient(created.clientId)
                is ToolExecutionResult.Failure -> return created
                else -> Unit
            }
        }
        client ?: return ToolExecutionResult.Failure(
            ToolName.QuickInvoiceFromUnbilledReceipts,
            FailureMessage("Client not found: ${arguments.clientName.value}")
        )
        val unbilled = when (val outcome = receiptRepository.getItemsByClient(client.name.value).first()) {
            is ReceiptListOutcome.Success -> outcome.receipts.filterNot { it.isBilled }
            is ReceiptListOutcome.Failure -> return ToolExecutionResult.Failure(
                ToolName.QuickInvoiceFromUnbilledReceipts,
                outcome.error
            )
        }
        if (unbilled.isEmpty()) {
            return ToolExecutionResult.Failure(
                ToolName.QuickInvoiceFromUnbilledReceipts,
                FailureMessage("No unbilled receipts for ${client.name}.")
            )
        }
        executeCreateDraftInvoice(CreateDraftInvoiceArgs(client.id))
        val lineItems = unbilled.map { receipt ->
            DraftLineItemInput(
                description = NaturalLanguage(
                    buildString {
                        append(receipt.description)
                        if (receipt.supplierName.isNotBlank()) append(" (${receipt.supplierName})")
                    }
                ),
                amount = receipt.totalPrice,
                category = NaturalLanguage("Materials")
            )
        }
        val update = executeUpdateDraftInvoice(
            UpdateDraftInvoiceArgs(
                clientName = NaturalLanguage(client.name.value),
                clientAddress = NaturalLanguage(client.address.value),
                lineItems = lineItems,
                replaceLineItems = true
            )
        )
        if (update is ToolExecutionResult.Failure) return update

        val currentDraft = draftRepository.getDraft().first()
        draftRepository.saveDraft(
            currentDraft.copy(
                linkedReceiptIds = unbilled.map { it.id.value }
            )
        )

        return ToolExecutionResult.DraftInvoiceUpdated(
            lineItemCount = unbilled.size,
            clientName = NaturalLanguage(client.name.value),
            uiEffects = listOf(
                AgentUiEffect.NavigateToTab(AppTab.NewInvoice),
                AgentUiEffect.SelectClient(client.id)
            )
        )
    }

    suspend fun executeDeleteInvoice(arguments: DeleteInvoiceApprovalArgs): ToolExecutionResult {
        val invoice = invoiceRepository.getInvoiceById(arguments.invoiceId)
            ?: return ToolExecutionResult.Failure(ToolName.DeleteInvoiceForApproval, FailureMessage("Invoice not found."))
        return when (invoiceRepository.deleteInvoice(invoice)) {
            is InvoiceOutcome.Success -> ToolExecutionResult.InvoiceDeletionQueued(
                invoice.id,
                uiEffects = listOf(AgentUiEffect.NavigateToTab(AppTab.History))
            )
            is InvoiceOutcome.Failure -> ToolExecutionResult.Failure(
                ToolName.DeleteInvoiceForApproval,
                FailureMessage("Delete failed.")
            )
        }
    }

    suspend fun executeOpenLastInvoice(arguments: OpenLastInvoiceArgs): ToolExecutionResult {
        val actualInvoice = if (arguments.invoiceId != null && arguments.invoiceId.value.isNotBlank()) {
            invoiceRepository.getInvoiceById(arguments.invoiceId)
        } else {
            invoiceRepository.searchInvoices("").takeIf { it.isNotEmpty() }?.maxByOrNull { it.lastUpdated }
                ?: invoiceRepository.allInvoices.first().maxByOrNull { it.lastUpdated }
        }

        if (actualInvoice == null || actualInvoice.pdfPath.value.isBlank()) {
            return ToolExecutionResult.Failure(
                ToolName.OpenLastInvoice,
                FailureMessage("No saved invoices found to open.")
            )
        }

        return ToolExecutionResult.OpenLastInvoiceCompleted(
            pdfPath = actualInvoice.pdfPath.value,
            uiEffects = listOf(
                AgentUiEffect.ViewPdf(actualInvoice.pdfPath.value)
            )
        )
    }

    suspend fun executeCreateChangeOrder(arguments: CreateChangeOrderArgs): ToolExecutionResult {
        val current = draftRepository.getDraft().first()
        val invoice = invoiceRepository.getInvoiceById(arguments.invoiceId)
        val clientName = invoice?.clientName?.value ?: current.clientName
        val clientAddress = invoice?.clientAddress?.value ?: current.clientAddress

        val newItem = LineItem(
            description = ItemsSummary("[Change Order] ${arguments.description.value}"),
            amount = MoneyAmount(arguments.amount),
            category = "Service"
        )
        val updated = current.copy(
            clientName = clientName,
            clientAddress = clientAddress,
            lineItems = current.lineItems + newItem
        )
        draftRepository.saveDraft(updated)

        return ToolExecutionResult.CreateChangeOrderCompleted(
            invoiceId = arguments.invoiceId,
            description = arguments.description,
            amount = arguments.amount,
            uiEffects = listOf(
                AgentUiEffect.NavigateToTab(AppTab.NewInvoice)
            )
        )
    }
}
