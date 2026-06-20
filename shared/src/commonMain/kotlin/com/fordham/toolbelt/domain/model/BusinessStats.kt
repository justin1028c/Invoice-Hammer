package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil

data class BusinessStats(
    val netProfit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalDurationSeconds: Long = 0L,
    val unbilledExpenses: Double = 0.0,
    val projectStats: List<ProjectStat> = emptyList(),
    val profitMargin: Int = 0,
    val ytdRevenue: Double = 0.0,
    val ytdExpenses: Double = 0.0,
    val ytdNetProfit: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val outstanding0to30: Double = 0.0,
    val outstanding31to60: Double = 0.0,
    val outstanding61Plus: Double = 0.0,
    val projectedTax: Double = 0.0,
    val currentQuarter: Int = 1
) {
    val formattedNetProfit: String get() = DateTimeUtil.formatMoney(netProfit)
    val formattedUnbilledExpenses: String get() = DateTimeUtil.formatMoney(unbilledExpenses)
    val formattedYtdRevenue: String get() = DateTimeUtil.formatMoney(ytdRevenue)
    val formattedYtdExpenses: String get() = DateTimeUtil.formatMoney(ytdExpenses)
    val formattedYtdNetProfit: String get() = DateTimeUtil.formatMoney(ytdNetProfit)
    val formattedTotalOutstanding: String get() = DateTimeUtil.formatMoney(totalOutstanding)
    val formattedOutstanding0to30: String get() = DateTimeUtil.formatMoney(outstanding0to30)
    val formattedOutstanding31to60: String get() = DateTimeUtil.formatMoney(outstanding31to60)
    val formattedOutstanding61Plus: String get() = DateTimeUtil.formatMoney(outstanding61Plus)
    val formattedProjectedTax: String get() = DateTimeUtil.formatMoney(projectedTax)
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
