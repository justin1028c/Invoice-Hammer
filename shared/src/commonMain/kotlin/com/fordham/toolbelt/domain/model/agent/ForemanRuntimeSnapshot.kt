package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.util.VoiceTranscriptMeta

/**
 * Immutable Foreman runtime hints for a single agent run (built from UI state).
 */
data class ForemanRuntimeSnapshot(
    val selectedClientId: ClientId? = null,
    val selectedClientName: String? = null,
    val knownClientsCatalog: String = "",
    val knownSuppliersCatalog: String = "",
    val pendingReceiptImageBytes: ByteArray? = null,
    val lastSavedInvoiceId: InvoiceId? = null,
    val lastSavedInvoiceClientName: String? = null,
    val voiceTranscriptMeta: VoiceTranscriptMeta? = null,
    val activeTab: AppTab? = null
) {
    fun withoutTransient(): ForemanRuntimeSnapshot = copy(
        pendingReceiptImageBytes = null,
        voiceTranscriptMeta = null
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ForemanRuntimeSnapshot) return false
        return selectedClientId == other.selectedClientId &&
            selectedClientName == other.selectedClientName &&
            knownClientsCatalog == other.knownClientsCatalog &&
            knownSuppliersCatalog == other.knownSuppliersCatalog &&
            pendingReceiptImageBytes.contentEquals(other.pendingReceiptImageBytes) &&
            lastSavedInvoiceId == other.lastSavedInvoiceId &&
            lastSavedInvoiceClientName == other.lastSavedInvoiceClientName &&
            voiceTranscriptMeta == other.voiceTranscriptMeta &&
            activeTab == other.activeTab
    }

    override fun hashCode(): Int {
        var result = selectedClientId?.hashCode() ?: 0
        result = 31 * result + (selectedClientName?.hashCode() ?: 0)
        result = 31 * result + knownClientsCatalog.hashCode()
        result = 31 * result + knownSuppliersCatalog.hashCode()
        result = 31 * result + (pendingReceiptImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (lastSavedInvoiceId?.hashCode() ?: 0)
        result = 31 * result + (lastSavedInvoiceClientName?.hashCode() ?: 0)
        result = 31 * result + (voiceTranscriptMeta?.hashCode() ?: 0)
        result = 31 * result + (activeTab?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun empty(): ForemanRuntimeSnapshot = ForemanRuntimeSnapshot()
    }
}

object ForemanRuntimeBinding {
    private var current: ForemanRuntimeSnapshot = ForemanRuntimeSnapshot.empty()

    fun bind(snapshot: ForemanRuntimeSnapshot) {
        current = snapshot
    }

    fun current(): ForemanRuntimeSnapshot = current

    fun recordLastSaved(invoiceId: InvoiceId, clientName: String) {
        current = current.copy(
            lastSavedInvoiceId = invoiceId,
            lastSavedInvoiceClientName = clientName
        )
    }

    fun clearTransient() {
        current = current.withoutTransient()
    }

    fun reset() {
        current = ForemanRuntimeSnapshot.empty()
    }
}
