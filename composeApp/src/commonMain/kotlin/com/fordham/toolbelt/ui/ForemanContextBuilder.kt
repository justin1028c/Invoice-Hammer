package com.fordham.toolbelt.ui

import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.agent.ForemanAppStateSnapshot
import com.fordham.toolbelt.domain.model.agent.ForemanAppStateSnapshotEncoder
import com.fordham.toolbelt.domain.model.agent.ForemanCatalogEntry
import com.fordham.toolbelt.domain.model.agent.ForemanClientSnapshot
import com.fordham.toolbelt.domain.model.agent.ForemanDraftLineSnapshot
import com.fordham.toolbelt.domain.model.agent.ForemanDraftSnapshot
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.ForemanSessionReducer
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceUiState

fun buildForemanSystemPrompt(
    tabIndex: Int,
    selectedClient: Client?,
    draft: NewInvoiceUiState,
    lastSavedInvoiceId: String?,
    lastSavedInvoiceClient: String?,
    pendingReceiptPhoto: Boolean,
    catalogClients: List<Client>,
    session: ForemanSession,
    voiceTranscriptMeta: com.fordham.toolbelt.util.VoiceTranscriptMeta? = null
): String {
    val tabName = com.fordham.toolbelt.domain.model.agent.AppTab.entries
        .firstOrNull { it.pageIndex == tabIndex }
        ?.navLabel
        ?: "UNKNOWN"
    val snapshot = ForemanAppStateSnapshot(
        tabIndex = tabIndex,
        tabName = tabName,
        selectedClient = selectedClient?.let {
            ForemanClientSnapshot(
                id = it.id.value,
                name = it.name,
                email = it.email.value,
                phone = it.phone.value,
                address = it.address
            )
        },
        draft = ForemanDraftSnapshot(
            clientName = draft.clientName,
            lineItemCount = draft.lineItems.size,
            taxPercent = draft.taxText,
            deposit = draft.depositCollected.toDoubleOrNull() ?: 0.0,
            lineItems = draft.lineItems.take(12).map {
                ForemanDraftLineSnapshot(it.description, it.amount, it.category)
            }
        ),
        lastSavedInvoiceId = lastSavedInvoiceId,
        lastSavedInvoiceClient = lastSavedInvoiceClient,
        pendingReceiptPhoto = pendingReceiptPhoto,
        clientCatalog = catalogClients.take(20).map {
            ForemanCatalogEntry(it.id.value, it.name)
        }
    )
    return buildString {
        append("You are the Foreman Brain for Invoice Hammer. Use tools to complete contractor requests.\n")
        append("[APP_STATE_JSON]\n")
        append(ForemanAppStateSnapshotEncoder.encode(snapshot))
        append("\nPrefer QUICK_INVOICE when client and amounts are clear. ")
        append("Use APPEND_DRAFT_LINES to add charges without wiping the draft. ")
        append("Use DUPLICATE_LAST_INVOICE when user says 'same as last time'. ")
        append("Use QUICK_INVOICE_FROM_UNBILLED_RECEIPTS to bill all receipt expenses for a client. ")
        if (pendingReceiptPhoto) {
            append("Receipt photo is available — SCAN_LAST_RECEIPT can OCR it. ")
        }
        val draftName = snapshot.draft?.clientName
        if (tabIndex == 0 && !draftName.isNullOrBlank()) {
            append("The user is actively viewing and editing the draft invoice for client '$draftName'. ")
            append("For commands like 'add labor', 'attach receipt', or 'send', DO NOT ask for client names, addresses, or other draft fields. Instead, assume the current draft context is active and run APPEND_DRAFT_LINES, SCAN_LAST_RECEIPT, or SAVE_INVOICE_FROM_DRAFT immediately without clarification. ")
        }
        append("If multiple clients match, the app will ask the user to pick — run SEARCH_CLIENTS first. ")
        append("Delete/Send require explicit user request.")
        
        // Dynamic active context entities from session
        append(ForemanSessionReducer.formatForContext(session))

        // Inject speech alternatives if voice recognition confidence is low
        val meta = voiceTranscriptMeta
        val confidence = meta?.confidence
        if (meta != null && confidence != null && confidence < 0.85f && meta.alternatives.isNotEmpty()) {
            append("\n\n[AUDIO HYPOTHESES] The audio transcription confidence is low (${(confidence * 100).toInt()}%). ")
            append("If the primary transcription has phonetic errors, try resolving against these alternative speech hypotheses:\n")
            meta.alternatives.forEach { alt ->
                append("- ").append(alt).append("\n")
            }
        }
    }

}

