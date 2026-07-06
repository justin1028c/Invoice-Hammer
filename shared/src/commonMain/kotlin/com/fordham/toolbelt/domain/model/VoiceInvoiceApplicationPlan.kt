package com.fordham.toolbelt.domain.model

data class VoiceInvoiceApplicationPlan(
    val clientName: String? = null,
    val clientAddress: String? = null,
    val taxRatePercent: Double? = null,
    val depositAmount: Double? = null,
    val hourlyRate: Double? = null,
    val pendingLineItems: List<LineItem> = emptyList(),
    val laborHours: Double? = null,
    val laborRate: Double? = null,
    val discountPercent: Double = 0.0,
    val notes: String = "",
    val confidenceScore: Double = 1.0,
    val userSummary: String = "",
    val validationIssues: List<String> = emptyList(),
    val requiresFollowUp: Boolean = false
)
