package com.fordham.toolbelt.domain.model

data class LineItem(
    val description: ItemsSummary,
    val amount: MoneyAmount,
    val category: String = "Service",
    val quantity: Double? = null,
    val unitPrice: MoneyAmount? = null
)
