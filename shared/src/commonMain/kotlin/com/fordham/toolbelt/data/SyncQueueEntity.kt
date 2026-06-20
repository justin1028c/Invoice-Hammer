package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fordham.toolbelt.util.randomUUID

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String = randomUUID(),
    val operationType: String,
    val createdAtMillis: Long,
    val retryCount: Int = 0
)
