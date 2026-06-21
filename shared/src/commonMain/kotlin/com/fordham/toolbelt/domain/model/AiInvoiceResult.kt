package com.fordham.toolbelt.domain.model

data class AiInvoiceResult(
    val clientName: String = "",
    val clientAddress: String = "",
    val items: List<LineItem> = emptyList(),
    val laborHours: Double? = null,
    val laborRate: Double? = null,
    val depositAmount: Double = 0.0,
    val taxRatePercent: Double = 7.0,
    val discountPercent: Double = 0.0,
    val notes: String = "",
    val confidenceScore: Double = 1.0,
    val userSummary: String = "",
    val validationIssues: List<String> = emptyList()
)
