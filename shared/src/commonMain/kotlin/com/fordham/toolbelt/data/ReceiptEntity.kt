package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID

@Entity(tableName = "receipt_items")
data class ReceiptEntity(
    @PrimaryKey val id: String = randomUUID(),
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double,
    val category: String = "Other",
    val clientName: String = "",
    val imagePath: String = "",
    val isBilled: Boolean = false,
    val linkedInvoiceId: String? = null,
    val lastUpdated: Long = DateTimeUtil.nowEpochMillis(),
    val isSynced: Boolean = false,
    val supplierName: String = "",
    val supplierId: String? = null
)
