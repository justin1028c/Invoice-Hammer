package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil

data class BusinessStats(
    val netProfit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalDurationSeconds: Long = 0L,
    val unbilledExpenses: Double = 0.0,
    val projectStats: List<ProjectStat> = emptyList(),
    val profitMargin: Int = 0
) {
    val formattedNetProfit: String get() = DateTimeUtil.formatMoney(netProfit)
    val formattedUnbilledExpenses: String get() = DateTimeUtil.formatMoney(unbilledExpenses)
}

data class ProjectStat(
    val clientName: String,
    val revenue: Double,
    val expenses: Double,
    val profit: Double,
    val progress: Float
) {
    val formattedProfit: String get() = DateTimeUtil.formatMoney(profit)
    val formattedRevenue: String get() = "REVENUE: ${DateTimeUtil.formatMoney(revenue)}"
    val formattedExpenses: String get() = "COSTS: ${DateTimeUtil.formatMoney(expenses)}"
}
