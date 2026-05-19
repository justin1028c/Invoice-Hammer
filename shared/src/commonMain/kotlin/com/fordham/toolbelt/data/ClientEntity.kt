package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fordham.toolbelt.util.randomUUID

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String = randomUUID(),
    val name: String,
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val totalInvoiced: Double = 0.0,
    val isFavorite: Boolean = false
)
