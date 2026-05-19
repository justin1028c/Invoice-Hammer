package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fordham.toolbelt.util.randomUUID

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String = randomUUID(),
    val name: String,
    val category: String,
    val address: String = "",
    val phone: String = "",
    val webUrl: String = "",
    val packageName: String = "",
    val displayOrder: Int,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val customLogoPath: String? = null,
    val logoResName: String? = null,
    val isDefault: Boolean = false,
    val totalSpendYtd: Double = 0.0,
    val jobsLinked: Int = 0,
    val avgMarkup: Double = 0.0
)
