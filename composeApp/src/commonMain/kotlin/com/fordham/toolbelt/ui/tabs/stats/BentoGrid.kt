package com.fordham.toolbelt.ui.tabs.stats

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.BusinessStats
import com.fordham.toolbelt.ui.theme.StatsGreen
import com.fordham.toolbelt.util.DateTimeUtil

/**
 * Responsibility: Layout orchestrator for the Bento-style statistics grid.
 */
@Composable
fun BentoGrid(stats: BusinessStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Main Row: Health & Margin
        Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoCard(
                modifier = Modifier.weight(1.8f),
                title = "NET PROFIT YTD",
                value = stats.formattedNetProfit,
                icon = Icons.Default.TrendingUp,
                color = StatsGreen,
                backgroundContent = {
                    TrendLineBackground(StatsGreen)
                }
            )
            BentoCard(
                modifier = Modifier.weight(1f),
                title = "MARGIN",
                value = "${stats.profitMargin}%",
                icon = Icons.Default.PieChart,
                content = {
                    Box(modifier = Modifier.size(70.dp).padding(top = 4.dp), contentAlignment = Alignment.Center) {
                        DonutChartSmall(stats.netProfit, stats.profitMargin)
                        Text("${stats.profitMargin}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }

        // Second Row: Hours & Unbilled
        Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val totalHours = stats.totalDurationSeconds / 3600.0
            BentoCard(
                modifier = Modifier.weight(1f),
                title = "TOTAL HOURS",
                value = DateTimeUtil.formatDecimal(totalHours, 1),
                subValue = "BILLABLE",
                icon = Icons.Default.Timer,
                color = MaterialTheme.colorScheme.primary,
                backgroundContent = {
                    TimePulseBackground(MaterialTheme.colorScheme.primary)
                }
            )
            BentoCard(
                modifier = Modifier.weight(1f),
                title = "UNBILLED",
                value = stats.formattedUnbilledExpenses,
                subValue = "FLOATING COSTS",
                icon = Icons.Default.ReceiptLong,
                color = MaterialTheme.colorScheme.secondary,
                backgroundContent = {
                    ReceiptDottedBackground(MaterialTheme.colorScheme.secondary)
                }
            )
        }
        
        // Third Row: Total Expenses
        BentoCard(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            title = "TOTAL BUSINESS EXPENSES",
            value = DateTimeUtil.formatMoney(stats.totalExpenses),
            icon = Icons.Default.Payments,
            isWide = true,
            color = MaterialTheme.colorScheme.primary,
            backgroundContent = {
                WavesBackground(MaterialTheme.colorScheme.primary)
            }
        )
    }
}
