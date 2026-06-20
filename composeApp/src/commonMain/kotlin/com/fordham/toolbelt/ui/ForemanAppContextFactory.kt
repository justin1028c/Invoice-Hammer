package com.fordham.toolbelt.ui

import com.fordham.toolbelt.domain.model.agent.ForemanAppContextBundle
import com.fordham.toolbelt.domain.model.agent.ForemanRuntimeSnapshot

/**
 * Builds immutable Foreman runtime + system prompt for one agent invocation.
 */
suspend fun buildForemanAppContextBundle(
    buildSystemPrompt: suspend () -> String,
    selectedClientId: com.fordham.toolbelt.domain.model.ClientId?,
    selectedClientName: String?,
    knownClientsCatalog: String,
    knownSuppliersCatalog: String,
    pendingReceiptImageBytes: ByteArray?,
    lastSavedInvoiceId: com.fordham.toolbelt.domain.model.InvoiceId?,
    lastSavedInvoiceClientName: String?,
    voiceTranscriptMeta: com.fordham.toolbelt.util.VoiceTranscriptMeta?,
    activeTab: com.fordham.toolbelt.domain.model.agent.AppTab? = null
): ForemanAppContextBundle {
    val runtime = ForemanRuntimeSnapshot(
        selectedClientId = selectedClientId,
        selectedClientName = selectedClientName,
        knownClientsCatalog = knownClientsCatalog,
        knownSuppliersCatalog = knownSuppliersCatalog,
        pendingReceiptImageBytes = pendingReceiptImageBytes,
        lastSavedInvoiceId = lastSavedInvoiceId,
        lastSavedInvoiceClientName = lastSavedInvoiceClientName,
        voiceTranscriptMeta = voiceTranscriptMeta,
        activeTab = activeTab
    )
    return ForemanAppContextBundle(
        systemPrompt = buildSystemPrompt(),
        runtime = runtime
    )
}
