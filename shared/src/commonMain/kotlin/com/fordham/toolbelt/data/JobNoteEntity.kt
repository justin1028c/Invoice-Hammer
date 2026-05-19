package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID

@Entity(tableName = "job_notes")
data class JobNoteEntity(
    @PrimaryKey val id: String = randomUUID(),
    val clientName: String = "",
    val invoiceId: String? = null,
    val text: String,
    val timestamp: Long = DateTimeUtil.nowEpochMillis(),
    val isSynced: Boolean = false
)
