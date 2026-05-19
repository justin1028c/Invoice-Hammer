package com.fordham.toolbelt.domain.model

data class Supplier(
    val id: SupplierId,
    val name: String,
    val category: SupplierCategory,
    val address: String = "",
    val phone: PhoneNumber = PhoneNumber(""),
    val webUrl: String = "",
    val packageName: String = "",
    val displayOrder: Int,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val customLogoPath: String? = null,
    val logoResName: String? = null,
    val isDefault: Boolean = false,
    val analytics: SupplierAnalytics = SupplierAnalytics()
)

enum class SupplierCategory {
    LUMBER, ELECTRICAL, PLUMBING, PAINT, ROOFING, HVAC, HARDWARE, FASTENERS, FLOORING, GENERAL_SUPPLY, OTHER
}

data class SupplierAnalytics(
    val totalSpendYtd: Double = 0.0,
    val jobsLinked: Int = 0,
    val avgMarkup: Double = 0.0
)
