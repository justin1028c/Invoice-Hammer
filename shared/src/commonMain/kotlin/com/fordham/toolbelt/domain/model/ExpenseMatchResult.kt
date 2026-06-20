package com.fordham.toolbelt.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExpenseMatchResult(
    val targetId: String,
    val clientName: String,
    val category: String,
    val isEstimate: Boolean,
    val totalAmount: Double
)
