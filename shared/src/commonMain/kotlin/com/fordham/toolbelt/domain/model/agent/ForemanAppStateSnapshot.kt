package com.fordham.toolbelt.domain.model.agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ForemanClientSnapshot(
    val id: String,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val address: String = ""
)

@Serializable
data class ForemanDraftLineSnapshot(
    val description: String,
    val amount: Double,
    val category: String = "Service"
)

@Serializable
data class ForemanDraftSnapshot(
    val clientName: String,
    val lineItemCount: Int,
    val taxPercent: String,
    val deposit: Double,
    val lineItems: List<ForemanDraftLineSnapshot> = emptyList()
)

@Serializable
data class ForemanCatalogEntry(
    val id: String,
    val name: String
)

@Serializable
data class ForemanAppStateSnapshot(
    val tabIndex: Int,
    val tabName: String,
    val selectedClient: ForemanClientSnapshot? = null,
    val draft: ForemanDraftSnapshot? = null,
    val lastSavedInvoiceId: String? = null,
    val lastSavedInvoiceClient: String? = null,
    val pendingReceiptPhoto: Boolean = false,
    val clientCatalog: List<ForemanCatalogEntry> = emptyList()
)

object ForemanAppStateSnapshotEncoder {
    private val json = Json { prettyPrint = false }

    fun encode(snapshot: ForemanAppStateSnapshot): String =
        json.encodeToString(snapshot)
}
