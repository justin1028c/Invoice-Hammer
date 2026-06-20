package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ClientAddress
import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.ClientListOutcome
import com.fordham.toolbelt.domain.model.EmailAddress
import com.fordham.toolbelt.domain.model.PhoneNumber
import com.fordham.toolbelt.domain.model.ProcessReceiptOutcome
import com.fordham.toolbelt.domain.model.ClientName
import com.fordham.toolbelt.domain.model.DraftInvoice
import com.fordham.toolbelt.domain.model.DurationSeconds
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.GenerateInvoiceOutcome
import com.fordham.toolbelt.domain.model.MediaUri
import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.domain.model.ReceiptId
import com.fordham.toolbelt.domain.model.TaxRatePercent
import com.fordham.toolbelt.domain.model.InvoiceOutcome
import com.fordham.toolbelt.domain.model.JobNoteOutcome
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.ReceiptImagePayload
import com.fordham.toolbelt.domain.model.Supplier
import com.fordham.toolbelt.domain.model.SupplierListOutcome
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.usecase.AddJobNoteUseCase
import com.fordham.toolbelt.domain.usecase.GenerateAndSaveInvoiceUseCase
import com.fordham.toolbelt.domain.usecase.GenerateInvoiceRequest
import com.fordham.toolbelt.domain.usecase.ProcessReceiptRequest
import com.fordham.toolbelt.domain.usecase.ProcessReceiptUseCase
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase
import com.fordham.toolbelt.domain.usecase.agent.*
import com.fordham.toolbelt.util.AppLogger
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.first
import kotlin.math.roundToLong

internal class RepositoryToolRegistryExecutions(
    private val clientRepository: ClientRepository,
    private val receiptRepository: ReceiptRepository,
    private val draftRepository: DraftRepository,
    private val invoiceRepository: InvoiceRepository,
    private val settingsRepository: SettingsRepository,
    private val supplierRepository: SupplierRepository,
    private val addJobNoteUseCase: AddJobNoteUseCase,
    private val generateAndSaveInvoiceUseCase: GenerateAndSaveInvoiceUseCase,
    private val processReceiptUseCase: ProcessReceiptUseCase,
    private val hasSubscriptionFeature: HasSubscriptionFeatureUseCase,
    private val getProfitGuardianStatus: GetProfitGuardianStatusUseCase,
    private val detectChangeOrders: DetectChangeOrdersUseCase,
    private val getDailyBriefing: GetDailyBriefingUseCase
) {
    private suspend fun resolveClient(clientId: com.fordham.toolbelt.domain.model.ClientId) =
        loadClients().firstOrNull { it.id == clientId }

    private suspend fun resolveClientByName(name: String) =
        loadClients().firstOrNull { it.name.equals(name, ignoreCase = true) }

    private suspend fun resolveClientOrSelected(
        clientId: ClientId?,
        clientName: String?
    ): com.fordham.toolbelt.domain.model.Client? {
        clientId?.let { resolveClient(it) }?.let { return it }
        
        if (!clientName.isNullOrBlank()) {
            val exactMatch = resolveClientByName(clientName)
            if (exactMatch != null) return exactMatch
            
            // Fuzzy match fallback
            val allClients = loadClients()
            var bestMatch: com.fordham.toolbelt.domain.model.Client? = null
            var bestScore = 0.0
            for (client in allClients) {
                val score = com.fordham.toolbelt.util.StringFuzzyMatcher.similarity(clientName, client.name)
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = client
                }
            }
            if (bestScore >= 0.8 && bestMatch != null) {
                return bestMatch
            }
            
            // If they explicitly named a client but we couldn't match them, 
            // do NOT fall back to active screen selection. Return null so the system knows this is a brand-new name.
            return null
        }
        
        // Only fall back to UI selection if NO client was explicitly requested in the command
        ForemanRuntimeBinding.current().selectedClientId?.let { resolveClient(it) }?.let { return it }
        ForemanRuntimeBinding.current().selectedClientName?.takeIf { it.isNotBlank() }?.let { resolveClientByName(it) }
        return null
    }

    private suspend fun loadClients() = when (val outcome = clientRepository.getAllClients().first()) {
        is ClientListOutcome.Success -> outcome.clients
        is ClientListOutcome.Failure -> {
            AppLogger.e("ToolRegistry", "Failed to load clients: ${outcome.error.value}")
            emptyList()
        }
    }

    suspend fun executeSearchClients(arguments: SearchClientsArgs): ToolExecutionResult {
        val query = arguments.query.value.trim()
        var clients = if (query.isBlank() || query.lowercase().contains("all") || query.lowercase().contains("recent") || query.lowercase().contains("last")) {
            loadClients().sortedByDescending { it.lastUpdated }
        } else {
            clientRepository.searchClients(query)
        }
        if (clients.isEmpty() && query.isNotBlank()) {
            val normalizedQuery = query.lowercase().replace("\\s".toRegex(), "")
            val allDbClients = loadClients()
            clients = allDbClients.filter { client ->
                val normalizedName = client.name.lowercase().replace("\\s".toRegex(), "")
                normalizedName.contains(normalizedQuery) || normalizedQuery.contains(normalizedName)
            }
        }
        if (clients.isEmpty() && query.isNotBlank()) {
            val normalizedQuery = query.lowercase().replace("\\s".toRegex(), "")
            val allInvoices = invoiceRepository.allInvoices.first()
            val matchedNames = allInvoices.map { it.clientName }
                .distinct()
                .filter { name ->
                    val normalizedName = name.lowercase().replace("\\s".toRegex(), "")
                    normalizedName.contains(normalizedQuery) || normalizedQuery.contains(normalizedName)
                }
            if (matchedNames.isNotEmpty()) {
                clients = matchedNames.map { name ->
                    val matchingInv = allInvoices.firstOrNull { it.clientName == name }
                    val newClient = Client(
                        id = ClientId(randomUUID()),
                        name = name,
                        email = matchingInv?.clientEmail ?: EmailAddress(""),
                        phone = matchingInv?.clientPhone ?: PhoneNumber(""),
                        address = matchingInv?.clientAddress ?: ""
                    )
                    // Auto-persist synthesized client so subsequent lookups by ID will succeed
                    clientRepository.insertClient(newClient)
                    newClient
                }
            }
        }
        val hits = clients.map { client ->
            val invoices = invoiceRepository.getInvoicesByClient(client.name).first()
            val totalInvoiced = invoices.filterNot { it.isEstimate }.sumOf { it.totalAmount }
            val totalOwed = invoices.filterNot { it.isEstimate || it.isPaid }.sumOf { it.totalAmount }
            ClientSearchHit(
                clientId = client.id,
                displayName = NaturalLanguage(client.name),
                totalInvoiced = totalInvoiced,
                totalOwed = totalOwed,
                invoiceCount = invoices.size,
                address = client.address
            )
        }
        val effects = buildList {
            if (hits.size == 1) {
                add(AgentUiEffect.NavigateToTab(AppTab.Clients))
                add(AgentUiEffect.SelectClient(hits.first().clientId))
            }
        }
        return ToolExecutionResult.ClientSearchCompleted(hits, effects)
    }

    suspend fun executeSelectClient(arguments: SelectClientArgs): ToolExecutionResult {
        val client = when {
            arguments.clientId != null -> resolveClient(arguments.clientId)
            !arguments.clientName?.value.isNullOrBlank() -> resolveClientByName(arguments.clientName!!.value)
            else -> null
        } ?: return ToolExecutionResult.Failure(
            ToolName.SelectClient,
            FailureMessage("Client not found.")
        )
        return ToolExecutionResult.ClientSelected(
            clientId = client.id,
            displayName = NaturalLanguage(client.name),
            uiEffects = listOf(
                AgentUiEffect.NavigateToTab(AppTab.Clients),
                AgentUiEffect.SelectClient(client.id)
            )
        )
    }

    suspend fun executeGetClientDetails(arguments: GetClientDetailsArgs): ToolExecutionResult {
        val client = resolveClient(arguments.clientId)
            ?: return ToolExecutionResult.Failure(ToolName.GetClientDetails, FailureMessage("Client not found."))
        val invoices = invoiceRepository.getInvoicesByClient(client.name).first()
        val summary = NaturalLanguage(
            buildString {
                append(client.name)
                append(" — ")
                append(client.address.ifBlank { "No address" })
                append(". Email: ${client.email.value.ifBlank { "n/a" }}")
                append(". Phone: ${client.phone.value.ifBlank { "n/a" }}")
                append(". Invoices on file: ${invoices.size}")
                val invoiced = (client.totalInvoiced * 100).roundToLong() / 100.0
                append(". Total invoiced: $$invoiced")
            }
        )
        return ToolExecutionResult.ClientDetailsLoaded(arguments.clientId, summary)
    }

    suspend fun executeGetUnbilledReceipts(arguments: GetUnbilledReceiptsArgs): ToolExecutionResult {
        val client = resolveClient(arguments.clientId)
            ?: return ToolExecutionResult.Failure(ToolName.GetUnbilledReceipts, FailureMessage("Client not found."))
        return when (val receipts = receiptRepository.getItemsByClient(client.name).first()) {
            is ReceiptListOutcome.Failure -> ToolExecutionResult.Failure(
                ToolName.GetUnbilledReceipts,
                receipts.error
            )
            is ReceiptListOutcome.Success -> ToolExecutionResult.UnbilledReceiptsFound(
                clientId = arguments.clientId,
                receipts = receipts.receipts.filterNot { it.isBilled }.map { receipt ->
                    UnbilledReceiptSummary(
                        receiptId = receipt.id,
                        supplierName = NaturalLanguage(receipt.supplierName.ifBlank { "General" }),
                        amount = CurrencyAmountCents((receipt.totalPrice * 100.0).roundToLong())
                    )
                },
                uiEffects = listOf(
                    AgentUiEffect.NavigateToTab(AppTab.Receipts),
                    AgentUiEffect.SelectClient(client.id)
                )
            )
        }
    }

    fun executeOpenTab(arguments: OpenTabArgs): ToolExecutionResult {
        return ToolExecutionResult.TabOpened(
            tab = arguments.tab,
            uiEffects = listOf(AgentUiEffect.NavigateToTab(arguments.tab))
        )
    }

    suspend fun executeCreateDraftInvoice(arguments: CreateDraftInvoiceArgs): ToolExecutionResult {
        val client = resolveClient(arguments.clientId)
            ?: return ToolExecutionResult.Failure(ToolName.CreateDraftInvoice, FailureMessage("Client not found."))
        draftRepository.saveDraft(
            DraftInvoice(
                clientName = client.name,
                clientAddress = client.address,
                taxRate = settingsRepository.getBusinessSettings().taxRate.takeIf { it > 0.0 } ?: 7.0,
                saveToClientDirectory = false
            )
        )
        return ToolExecutionResult.DraftInvoiceCreated(
            invoiceId = com.fordham.toolbelt.domain.model.InvoiceId("draft-pending"),
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

    suspend fun executeAddJobNote(arguments: AddJobNoteArgs): ToolExecutionResult {
        return when (addJobNoteUseCase(arguments.clientName.value, arguments.note.value)) {
            is JobNoteOutcome.Success -> ToolExecutionResult.JobNoteAdded(
                clientName = arguments.clientName,
                uiEffects = listOf(AgentUiEffect.NavigateToTab(AppTab.Clients))
            )
            is JobNoteOutcome.Failure -> ToolExecutionResult.Failure(
                ToolName.AddJobNote,
                FailureMessage("Could not save job note.")
            )
        }
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
                val subtotal = draft.lineItems.sumOf { it.amount }
                val tax = subtotal * (draft.taxRate / 100.0)
                val savedInvoice = invoiceRepository.getInvoicesByClient(draft.clientName).first()
                    .maxByOrNull { it.lastUpdated }
                val invoiceId = savedInvoice?.id ?: com.fordham.toolbelt.domain.model.InvoiceId(randomUUID())
                val clientRecord = resolveClientByName(draft.clientName)
                val emailStr = clientRecord?.email?.value.orEmpty()
                val phoneStr = clientRecord?.phone?.value.orEmpty()
                val pdf = savedPath.ifBlank { savedInvoice?.pdfPath.orEmpty() }

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
                "id=${inv.id.value}, client=${inv.clientName}, total=${com.fordham.toolbelt.util.DateTimeUtil.formatMoney(inv.totalAmount)}, date=${inv.date}, paid=${inv.isPaid}"
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

    suspend fun executeCreateClient(arguments: CreateClientArgs): ToolExecutionResult {
        val name = arguments.clientName.value.trim()
        if (name.isBlank()) {
            return ToolExecutionResult.Failure(ToolName.CreateClient, FailureMessage("Client name is required."))
        }
        resolveClientByName(name)?.let { existing ->
            return ToolExecutionResult.ClientCreated(
                clientId = existing.id,
                displayName = NaturalLanguage(existing.name),
                uiEffects = listOf(
                    AgentUiEffect.NavigateToTab(AppTab.Clients),
                    AgentUiEffect.SelectClient(existing.id)
                )
            )
        }
        val client = com.fordham.toolbelt.domain.model.Client(
            name = name,
            email = arguments.email,
            phone = arguments.phone,
            address = arguments.address.value
        )
        return when (clientRepository.insertClient(client)) {
            is com.fordham.toolbelt.domain.model.ClientOutcome.Success -> ToolExecutionResult.ClientCreated(
                clientId = client.id,
                displayName = NaturalLanguage(client.name),
                uiEffects = listOf(
                    AgentUiEffect.NavigateToTab(AppTab.Clients),
                    AgentUiEffect.SelectClient(client.id)
                )
            )
            is com.fordham.toolbelt.domain.model.ClientOutcome.Failure ->
                ToolExecutionResult.Failure(ToolName.CreateClient, FailureMessage("Could not create client."))
        }
    }

    suspend fun executeScanLastReceipt(arguments: ScanLastReceiptArgs): ToolExecutionResult {
        val bytes = ForemanRuntimeBinding.current().pendingReceiptImageBytes
            ?: return ToolExecutionResult.Failure(
                ToolName.ScanLastReceipt,
                FailureMessage("No receipt photo ready. Open Receipts and capture an image first.")
            )
        if (!hasSubscriptionFeature(SubscriptionFeature.ReceiptOcr)) {
            return ToolExecutionResult.Failure(
                ToolName.ScanLastReceipt,
                FailureMessage("Pro subscription required for receipt scan.")
            )
        }
        val result = processReceiptUseCase(
            ProcessReceiptRequest(
                imageBytes = ReceiptImagePayload(bytes),
                clientName = arguments.clientName
            )
        )
        return when (result) {
            is ProcessReceiptOutcome.Success -> {
                val draft = draftRepository.getDraft().first()
                if (draft.clientName.isNotBlank()) {
                    val lineItems = result.items.map { item ->
                        com.fordham.toolbelt.domain.model.LineItem(
                            description = if (item.supplierName.isNotBlank()) "${item.description} (${item.supplierName})" else item.description,
                            amount = item.totalPrice,
                            category = "Materials",
                            quantity = item.quantity,
                            unitPrice = item.unitPrice
                        )
                    }
                    draftRepository.saveDraft(
                        draft.copy(
                            lineItems = draft.lineItems + lineItems,
                            linkedReceiptIds = draft.linkedReceiptIds + result.items.map { it.id.value }
                        )
                    )
                    result.items.forEach { item ->
                        receiptRepository.updateItem(
                            item.copy(
                                isBilled = true,
                                linkedInvoiceId = com.fordham.toolbelt.domain.model.InvoiceId("current_draft")
                            )
                        )
                    }
                    ToolExecutionResult.ReceiptScanned(
                        itemCount = result.items.size,
                        uiEffects = listOf(
                            AgentUiEffect.NavigateToTab(AppTab.NewInvoice)
                        )
                    )
                } else {
                    ToolExecutionResult.ReceiptScanned(
                        itemCount = result.items.size,
                        uiEffects = listOf(AgentUiEffect.NavigateToTab(AppTab.Receipts))
                    )
                }
            }
            is ProcessReceiptOutcome.PremiumRequired -> ToolExecutionResult.Failure(
                ToolName.ScanLastReceipt,
                FailureMessage("Pro subscription required for receipt scan.")
            )
            is ProcessReceiptOutcome.Failure -> ToolExecutionResult.Failure(ToolName.ScanLastReceipt, result.error)
        }
    }

    suspend fun executeQuickClientLookup(arguments: QuickClientLookupArgs): ToolExecutionResult {
        val search = executeSearchClients(SearchClientsArgs(arguments.query))
        if (search is ToolExecutionResult.ClientSearchCompleted && search.clients.isNotEmpty()) {
            val details = executeGetClientDetails(GetClientDetailsArgs(search.clients.first().clientId))
            if (details is ToolExecutionResult.ClientDetailsLoaded) {
                return ToolExecutionResult.QuickClientLookupCompleted(
                    summary = details.summary,
                    uiEffects = details.uiEffects
                )
            }
        }
        return ToolExecutionResult.QuickClientLookupCompleted(
            summary = NaturalLanguage(
                if (search is ToolExecutionResult.ClientSearchCompleted && search.clients.isEmpty()) {
                    "No clients matched '${arguments.query.value}'."
                } else {
                    "Could not load client details."
                }
            ),
            uiEffects = emptyList()
        )
    }

    suspend fun executeQuickInvoice(arguments: QuickInvoiceArgs): ToolExecutionResult {
        var client = resolveClientOrSelected(null, arguments.clientName.value)
        if (client == null && arguments.createClientIfMissing) {
            when (val created = executeCreateClient(
                CreateClientArgs(
                    clientName = arguments.clientName,
                    address = arguments.clientAddress
                )
            )) {
                is ToolExecutionResult.ClientCreated -> client = resolveClient(created.clientId)
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
                    clientName = NaturalLanguage(client.name),
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
            clientName = NaturalLanguage(client.name),
            uiEffects = listOf(
                AgentUiEffect.NavigateToTab(AppTab.NewInvoice),
                AgentUiEffect.SelectClient(client.id)
            )
        )
    }

    suspend fun executeQuickClientAndInvoice(arguments: QuickClientAndInvoiceArgs): ToolExecutionResult {
        val clientExisted = resolveClientOrSelected(null, arguments.clientName.value) != null
        if (!clientExisted) {
            val created = executeCreateClient(
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
            val existing = resolveClientByName(arguments.clientName.value)
            if (existing != null) {
                val mergedPhone = if (arguments.clientPhone.value.isNotBlank()) arguments.clientPhone else existing.phone
                val mergedEmail = if (arguments.clientEmail.value.isNotBlank()) arguments.clientEmail else existing.email
                val mergedAddress = if (arguments.clientAddress.value.isNotBlank()) arguments.clientAddress.value else existing.address
                clientRepository.insertClient(
                    existing.copy(
                        address = mergedAddress,
                        phone = mergedPhone,
                        email = mergedEmail,
                        lastUpdated = com.fordham.toolbelt.util.DateTimeUtil.nowEpochMillis()
                    )
                )
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
        val client = resolveClientOrSelected(null, arguments.clientName.value)
            ?: return ToolExecutionResult.Failure(
                ToolName.DuplicateLastInvoice,
                FailureMessage("Client not found: ${arguments.clientName.value}")
            )
        val last = invoiceRepository.getInvoicesByClient(client.name).first()
            .maxByOrNull { it.lastUpdated }
            ?: return ToolExecutionResult.Failure(
                ToolName.DuplicateLastInvoice,
                FailureMessage("No prior invoice for ${client.name}.")
            )
        executeCreateDraftInvoice(CreateDraftInvoiceArgs(client.id))
        val description = last.itemsSummary.takeIf { it.isNotBlank() }
            ?: "Repeat of invoice ${last.date}"
        return executeUpdateDraftInvoice(
            UpdateDraftInvoiceArgs(
                clientName = NaturalLanguage(client.name),
                clientAddress = NaturalLanguage(client.address),
                lineItems = listOf(
                    DraftLineItemInput(
                        description = NaturalLanguage(description),
                        amount = last.totalAmount,
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
        var client = resolveClientOrSelected(null, arguments.clientName.value)
        if (client == null && arguments.createClientIfMissing) {
            when (val created = executeCreateClient(
                CreateClientArgs(clientName = arguments.clientName)
            )) {
                is ToolExecutionResult.ClientCreated -> client = resolveClient(created.clientId)
                is ToolExecutionResult.Failure -> return created
                else -> Unit
            }
        }
        client ?: return ToolExecutionResult.Failure(
            ToolName.QuickInvoiceFromUnbilledReceipts,
            FailureMessage("Client not found: ${arguments.clientName.value}")
        )
        val unbilled = when (val outcome = receiptRepository.getItemsByClient(client.name).first()) {
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
                clientName = NaturalLanguage(client.name),
                clientAddress = NaturalLanguage(client.address),
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
            clientName = NaturalLanguage(client.name),
            uiEffects = listOf(
                AgentUiEffect.NavigateToTab(AppTab.NewInvoice),
                AgentUiEffect.SelectClient(client.id)
            )
        )
    }

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
        if (invoice.pdfPath.isBlank()) {
            return ToolExecutionResult.Failure(ToolName.SendInvoiceEmail, FailureMessage("Invoice has no PDF yet."))
        }
        return ToolExecutionResult.InvoiceSendQueued(
            invoiceId = invoice.id,
            channel = NaturalLanguage("email"),
            toolName = ToolName.SendInvoiceEmail,
            uiEffects = listOf(
                AgentUiEffect.ShareInvoiceDocument(
                    pdfPath = invoice.pdfPath,
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
        if (invoice.pdfPath.isBlank()) {
            return ToolExecutionResult.Failure(ToolName.SendInvoiceSms, FailureMessage("Invoice has no PDF yet."))
        }
        return ToolExecutionResult.InvoiceSendQueued(
            invoiceId = invoice.id,
            channel = NaturalLanguage("sms"),
            toolName = ToolName.SendInvoiceSms,
            uiEffects = listOf(
                AgentUiEffect.ShareInvoiceDocument(
                    pdfPath = invoice.pdfPath,
                    title = "Invoice",
                    recipientPhone = arguments.recipientPhone.value,
                    body = arguments.message.value
                )
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

        if (actualInvoice == null || actualInvoice.pdfPath.isBlank()) {
            return ToolExecutionResult.Failure(
                ToolName.OpenLastInvoice,
                FailureMessage("No saved invoices found to open.")
            )
        }

        return ToolExecutionResult.OpenLastInvoiceCompleted(
            pdfPath = actualInvoice.pdfPath,
            uiEffects = listOf(
                AgentUiEffect.ViewPdf(actualInvoice.pdfPath)
            )
        )
    }

    suspend fun executeOpenSupplier(arguments: OpenSupplierArgs): ToolExecutionResult {
        val suppliers = when (val outcome = supplierRepository.getVisibleSuppliers().first()) {
            is SupplierListOutcome.Success -> outcome.suppliers
            is SupplierListOutcome.Failure -> return ToolExecutionResult.Failure(
                ToolName.OpenSupplier,
                FailureMessage("Failed to load suppliers list: ${outcome.error.value}")
            )
        }

        val matchedSupplier = when {
            arguments.supplierId != null -> {
                suppliers.firstOrNull { it.id == arguments.supplierId }
            }
            arguments.supplierName != null -> {
                val queryName = arguments.supplierName.value.trim()
                suppliers.firstOrNull { it.name.equals(queryName, ignoreCase = true) }
                    ?: suppliers.firstOrNull { it.name.contains(queryName, ignoreCase = true) }
            }
            else -> null
        } ?: return ToolExecutionResult.Failure(
            ToolName.OpenSupplier,
            FailureMessage("Supplier not found.")
        )

        return ToolExecutionResult.OpenSupplierCompleted(
            supplierId = matchedSupplier.id.value,
            name = matchedSupplier.name,
            packageName = matchedSupplier.packageName,
            webUrl = matchedSupplier.webUrl,
            uiEffects = listOf(
                AgentUiEffect.NavigateToTab(AppTab.Suppliers),
                AgentUiEffect.OpenSupplierStore(
                    supplierId = matchedSupplier.id.value,
                    name = matchedSupplier.name,
                    packageName = matchedSupplier.packageName,
                    webUrl = matchedSupplier.webUrl
                )
            )
        )
    }

    suspend fun executeGetProfitGuardianStatus(arguments: GetProfitGuardianStatusArgs): ToolExecutionResult {
        return when (val outcome = getProfitGuardianStatus(arguments.invoiceId)) {
            is ProfitGuardianOutcome.Success -> ToolExecutionResult.GetProfitGuardianStatusCompleted(outcome.status)
            is ProfitGuardianOutcome.ProjectNotFound -> ToolExecutionResult.Failure(
                ToolName.GetProfitGuardianStatus,
                FailureMessage("Estimate/Invoice project not found: ${arguments.invoiceId.value}")
            )
            is ProfitGuardianOutcome.Error -> ToolExecutionResult.Failure(
                ToolName.GetProfitGuardianStatus,
                outcome.message
            )
        }
    }

    suspend fun executeDetectChangeOrders(arguments: DetectChangeOrdersArgs): ToolExecutionResult {
        return when (val outcome = detectChangeOrders(arguments.invoiceId)) {
            is DetectChangeOrdersOutcome.Success -> ToolExecutionResult.DetectChangeOrdersCompleted(outcome.opportunities)
            is DetectChangeOrdersOutcome.ProjectNotFound -> ToolExecutionResult.Failure(
                ToolName.DetectChangeOrders,
                FailureMessage("Estimate/Invoice project not found: ${arguments.invoiceId.value}")
            )
            is DetectChangeOrdersOutcome.Error -> ToolExecutionResult.Failure(
                ToolName.DetectChangeOrders,
                outcome.message
            )
        }
    }

    suspend fun executeGetDailyBriefing(arguments: GetDailyBriefingArgs): ToolExecutionResult {
        return when (val outcome = getDailyBriefing.execute()) {
            is GetDailyBriefingOutcome.Success -> ToolExecutionResult.GetDailyBriefingCompleted(outcome.briefing)
            is GetDailyBriefingOutcome.Error -> ToolExecutionResult.Failure(
                ToolName.GetDailyBriefing,
                outcome.message
            )
        }
    }

    suspend fun executeCreateChangeOrder(arguments: CreateChangeOrderArgs): ToolExecutionResult {
        val current = draftRepository.getDraft().first()
        val invoice = invoiceRepository.getInvoiceById(arguments.invoiceId)
        val clientName = invoice?.clientName ?: current.clientName
        val clientAddress = invoice?.clientAddress ?: current.clientAddress

        val newItem = com.fordham.toolbelt.domain.model.LineItem(
            description = "[Change Order] ${arguments.description.value}",
            amount = arguments.amount,
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

