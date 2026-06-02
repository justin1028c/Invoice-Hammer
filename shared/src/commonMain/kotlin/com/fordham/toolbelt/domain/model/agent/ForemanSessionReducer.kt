package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.InvoiceId

/**
 * Updates typed session memory after each tool run so the LLM reuses real ids.
 */
object ForemanSessionReducer {
    fun apply(
        session: ForemanSession,
        toolName: ToolName,
        result: ToolExecutionResult,
        @Suppress("UNUSED_PARAMETER") arguments: ToolArguments
    ): ForemanSession {
        if (result is ToolExecutionResult.Failure) return session

        var next = session
        when (result) {
            is ToolExecutionResult.ClientSearchCompleted -> {
                if (result.clients.size == 1) {
                    val hit = result.clients.first()
                    next = rememberClient(next, hit.clientId, hit.displayName)
                }
            }
            is ToolExecutionResult.ClientSelected ->
                next = rememberClient(next, result.clientId, result.displayName)
            is ToolExecutionResult.ClientCreated ->
                next = rememberClient(next, result.clientId, result.displayName)
            is ToolExecutionResult.DraftInvoiceCreated ->
                next = next.copy(
                    activeClient = result.clientId,
                    activeDraftInvoice = result.invoiceId
                )
            is ToolExecutionResult.DraftInvoiceUpdated -> {
                next.activeClient?.let { clientId ->
                    next = next.copy(
                        resolvedEntities = next.resolvedEntities.remember(
                            result.clientName,
                            ResolvedClient(clientId)
                        )
                    )
                }
            }
            is ToolExecutionResult.InvoiceSavedFromDraft ->
                next = rememberInvoice(next, result.invoiceId, result.clientName)
            is ToolExecutionResult.QuickInvoiceCompleted ->
                next = rememberInvoice(next, result.invoiceId, result.clientName)
            is ToolExecutionResult.QuickClientAndInvoiceCompleted ->
                next = rememberInvoice(next, result.invoiceId, result.clientName)
            is ToolExecutionResult.QuickInvoiceFromUnbilledCompleted ->
                next = rememberInvoice(next, result.invoiceId, result.clientName)
            else -> Unit
        }
        return next
    }

    fun formatForContext(session: ForemanSession): String {
        if (session.activeClient == null && session.activeDraftInvoice == null &&
            session.resolvedEntities.entries().isEmpty()
        ) {
            return ""
        }
        return buildString {
            append("\n[SESSION MEMORY]\n")
            session.activeClient?.let { append("active_client_id=").append(it.value).append('\n') }
            session.activeDraftInvoice?.let { append("active_draft_invoice_id=").append(it.value).append('\n') }
            session.resolvedEntities.entries().take(8).forEach { entry ->
                when (val entity = entry.entity) {
                    is ResolvedClient ->
                        append("resolved: \"").append(entry.alias.value)
                            .append("\" -> client_id=").append(entity.id.value).append('\n')
                    is ResolvedInvoice ->
                        append("resolved: \"").append(entry.alias.value)
                            .append("\" -> invoice_id=").append(entity.id.value).append('\n')
                    is ResolvedReceipt ->
                        append("resolved: \"").append(entry.alias.value)
                            .append("\" -> receipt_id=").append(entity.id.value).append('\n')
                }
            }
            append("Use these ids in the next tool call — do not invent ids.\n")
        }
    }

    private fun rememberClient(
        session: ForemanSession,
        clientId: ClientId,
        displayName: NaturalLanguage
    ): ForemanSession {
        return session.copy(
            activeClient = clientId,
            resolvedEntities = session.resolvedEntities.remember(displayName, ResolvedClient(clientId))
        )
    }

    private fun rememberInvoice(
        session: ForemanSession,
        invoiceId: InvoiceId,
        clientName: NaturalLanguage
    ): ForemanSession {
        return session.copy(
            activeDraftInvoice = invoiceId,
            resolvedEntities = session.resolvedEntities
                .remember(clientName, ResolvedInvoice(invoiceId))
                .remember(NaturalLanguage("last_invoice"), ResolvedInvoice(invoiceId))
        )
    }
}
