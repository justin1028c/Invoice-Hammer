package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
data class DraftInvoiceEntity(
    @PrimaryKey val id: String = "current_draft",
    val clientName: String = "",
    val clientAddress: String = "",
    val taxRate: Double = 7.0,
    val deposit: Double = 0.0,
    val hourlyRate: Double = 50.0,
    val logoUri: String? = null,
    val selectedCategory: String = "Drywall",
    val itemDesc: String = "",
    val itemAmt: String = "",
    val elapsedSeconds: Long = 0L,
    val startTime: Long = 0L,
    val timerRunning: Boolean = false,
    val saveToClientDirectory: Boolean = false,
    val lineItemsJson: String = "[]",
    val capturedPhotosJson: String = "[]",
    val linkedReceiptIdsJson: String = "[]"
)
