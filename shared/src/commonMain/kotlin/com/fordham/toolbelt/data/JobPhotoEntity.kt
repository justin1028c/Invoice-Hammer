package com.fordham.toolbelt.data

import androidx.room.*
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID

@Entity(
    tableName = "job_photos",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class JobPhotoEntity(
    @PrimaryKey val id: String = randomUUID(),
    val invoiceId: String,
    val localUri: String,
    val phase: String = "BEFORE",
    val timestamp: Long = DateTimeUtil.nowEpochMillis()
)
