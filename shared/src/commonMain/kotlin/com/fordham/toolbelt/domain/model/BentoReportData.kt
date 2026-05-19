package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID

data class BentoReportData(
    val netProfit: Double,
    val grossIncome: Double,
    val expenses: Double,
    val invoices: List<Invoice>,
    val receiptCount: Int,
    val dateGeneratedMillis: Long = DateTimeUtil.nowEpochMillis(),
    val reportId: String = randomUUID(),
    val businessName: String = "Invoice Hammer"
)
