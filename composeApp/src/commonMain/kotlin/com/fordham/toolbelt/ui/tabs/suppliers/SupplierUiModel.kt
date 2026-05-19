package com.fordham.toolbelt.ui.tabs.suppliers

import com.fordham.toolbelt.domain.model.Supplier

/**
 * Responsibility: UI-specific representation of a Supplier.
 */
data class SupplierUiModel(
    val domain: Supplier,
    val logoKey: String? = null,
    val categoryIconKey: String? = null
)
