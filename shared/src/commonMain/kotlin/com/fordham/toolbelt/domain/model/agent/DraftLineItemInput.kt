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
        description = com.fordham.toolbelt.domain.model.ItemsSummary(description.value),
        amount = com.fordham.toolbelt.domain.model.MoneyAmount(amount),
        category = category.value.ifBlank { "Service" },
        quantity = quantity,
        unitPrice = unitPrice?.let { com.fordham.toolbelt.domain.model.MoneyAmount(it) }
    )
}
