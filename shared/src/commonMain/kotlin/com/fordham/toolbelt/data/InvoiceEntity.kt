package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String = randomUUID(),
    val clientName: String,
    val clientAddress: String,
    val clientPhone: String = "",
    val clientEmail: String = "",
    val date: String,
    val totalAmount: Double,
    val depositAmount: Double = 0.0,
    val itemsSummary: String,
    val pdfPath: String = "",
    val isPaid: Boolean = false,
    val isEstimate: Boolean = false,
    val lastUpdated: Long = DateTimeUtil.nowEpochMillis(),
    val durationSeconds: Long = 0L,
    val isSynced: Boolean = false
)
