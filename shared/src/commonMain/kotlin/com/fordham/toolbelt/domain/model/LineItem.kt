package com.fordham.toolbelt.domain.model

data class LineItem(
    val description: String,
    val amount: Double,
    val category: String = "Service"
)
