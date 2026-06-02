package com.fordham.toolbelt.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class PowerPayWebhookEventDto(
    val type: String,
    val invoiceId: String? = null,
    val projectId: String? = null,
    val milestoneId: String? = null,
    val amount: Double? = null,
    val receiptUrl: String? = null,
    val transactionHash: String? = null,
    val explorerUrl: String? = null,
    val eventId: String? = null
)
