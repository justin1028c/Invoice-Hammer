package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.LineItem

data class DraftLineItemInput(
    val description: NaturalLanguage,
    val amount: Double,
    val category: NaturalLanguage,
    val quantity: Double? = null,
    val unitPrice: Double? = null
) {
    fun toLineItem(): LineItem = LineItem(
        description = description.value,
        amount = amount,
        category = category.value.ifBlank { "Service" },
        quantity = quantity,
        unitPrice = unitPrice
    )
}
