package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.util.AppLogger
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.first
import kotlin.math.roundToLong

import com.fordham.toolbelt.domain.usecase.AddJobNoteUseCase
import com.fordham.toolbelt.domain.model.EmailAddress

class ClientToolExecutions(
    private val clientRepository: ClientRepository,
    private val invoiceRepository: InvoiceRepository,
    private val addJobNoteUseCase: AddJobNoteUseCase
) {
    suspend fun loadClients(): List<Client> = when (val outcome = clientRepository.getAllClients().first()) {
        is ClientListOutcome.Success -> outcome.clients
        is ClientListOutcome.Failure -> {
            AppLogger.e("ClientToolExecutions", "Failed to load clients: ${outcome.error.value}")
            emptyList()
        }
    }

    suspend fun resolveClient(clientId: ClientId): Client? =
        loadClients().firstOrNull { it.id == clientId }

    suspend fun resolveClientByName(name: String): Client? =
        loadClients().firstOrNull { it.name.value.equals(name, ignoreCase = true) }

    suspend fun resolveClientOrSelected(
        clientId: ClientId?,
        clientName: String?
    ): Client? {
        clientId?.let { resolveClient(it) }?.let { return it }
        
        if (!clientName.isNullOrBlank()) {
            val exactMatch = resolveClientByName(clientName)
            if (exactMatch != null) return exactMatch
            
            // Fuzzy match fallback
            val allClients = loadClients()
            var bestMatch: Client? = null
            var bestScore = 0.0
            for (client in allClients) {
                val score = com.fordham.toolbelt.util.StringFuzzyMatcher.similarity(clientName, client.name.value)
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
                val normalizedName = client.name.value.lowercase().replace("\\s".toRegex(), "")
                normalizedName.contains(normalizedQuery) || normalizedQuery.contains(normalizedName)
            }
        }
        if (clients.isEmpty() && query.isNotBlank()) {
            val normalizedQuery = query.lowercase().replace("\\s".toRegex(), "")
            val allInvoices = invoiceRepository.allInvoices.first()
            val matchedNames = allInvoices.map { it.clientName }
                .distinct()
                .filter { name ->
                    val normalizedName = name.value.lowercase().replace("\\s".toRegex(), "")
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
                        address = matchingInv?.clientAddress ?: ClientAddress("")
                    )
                    // Auto-persist synthesized client so subsequent lookups by ID will succeed
                    clientRepository.insertClient(newClient)
                    newClient
                }
            }
        }
        val hits = clients.map { client ->
            val invoices = invoiceRepository.getInvoicesByClient(client.name.value).first()
            val totalInvoiced = invoices.filterNot { it.isEstimate }.map { it.totalAmount.value }.sum()
            val totalOwed = invoices.filterNot { it.isEstimate || it.isPaid }.map { it.totalAmount.value }.sum()
            ClientSearchHit(
                clientId = client.id,
                displayName = NaturalLanguage(client.name.value),
                totalInvoiced = totalInvoiced,
                totalOwed = totalOwed,
                invoiceCount = invoices.size,
                address = client.address.value
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
            displayName = NaturalLanguage(client.name.value),
            uiEffects = listOf(
                AgentUiEffect.NavigateToTab(AppTab.Clients),
                AgentUiEffect.SelectClient(client.id)
            )
        )
    }

    suspend fun executeGetClientDetails(arguments: GetClientDetailsArgs): ToolExecutionResult {
        val client = resolveClient(arguments.clientId)
            ?: return ToolExecutionResult.Failure(ToolName.GetClientDetails, FailureMessage("Client not found."))
        val invoices = invoiceRepository.getInvoicesByClient(client.name.value).first()
        val summary = NaturalLanguage(
            buildString {
                append(client.name.value)
                append(" — ")
                append(client.address.value.ifBlank { "No address" })
                append(". Email: ${client.email.value.ifBlank { "n/a" }}")
                append(". Phone: ${client.phone.value.ifBlank { "n/a" }}")
                append(". Invoices on file: ${invoices.size}")
                val invoiced = (client.totalInvoiced.value * 100).roundToLong() / 100.0
                append(". Total invoiced: $$invoiced")
            }
        )
        return ToolExecutionResult.ClientDetailsLoaded(arguments.clientId, summary)
    }

    suspend fun executeCreateClient(arguments: CreateClientArgs): ToolExecutionResult {
        val name = arguments.clientName.value.trim()
        if (name.isBlank()) {
            return ToolExecutionResult.Failure(ToolName.CreateClient, FailureMessage("Client name is required."))
        }
        resolveClientByName(name)?.let { existing ->
            return ToolExecutionResult.ClientCreated(
                clientId = existing.id,
                displayName = NaturalLanguage(existing.name.value),
                uiEffects = listOf(
                    AgentUiEffect.NavigateToTab(AppTab.Clients),
                    AgentUiEffect.SelectClient(existing.id)
                )
            )
        }
        val client = Client(
            name = ClientName(name),
            email = arguments.email,
            phone = arguments.phone,
            address = ClientAddress(arguments.address.value)
        )
        return when (clientRepository.insertClient(client)) {
            is ClientOutcome.Success -> ToolExecutionResult.ClientCreated(
                clientId = client.id,
                displayName = NaturalLanguage(client.name.value),
                uiEffects = listOf(
                    AgentUiEffect.NavigateToTab(AppTab.Clients),
                    AgentUiEffect.SelectClient(client.id)
                )
            )
            is ClientOutcome.Failure ->
                ToolExecutionResult.Failure(ToolName.CreateClient, FailureMessage("Could not create client."))
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

    suspend fun executeAddJobNote(arguments: AddJobNoteArgs): ToolExecutionResult {
        return when (val outcome = addJobNoteUseCase(arguments.clientName.value, arguments.note.value)) {
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
}
