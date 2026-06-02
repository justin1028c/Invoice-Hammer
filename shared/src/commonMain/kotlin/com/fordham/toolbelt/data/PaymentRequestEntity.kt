package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_requests")
data class PaymentRequestEntity(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val invoiceClientName: String,
    val type: String,
    val provider: String,
    val requestedAmount: Double,
    val status: String,
    val paymentLink: String,
    val createdAtMillis: Long,
    val paidAtMillis: Long?,
    val stellarTransactionHash: String?,
    val stellarExplorerUrl: String?,
    val assetCode: String
)
