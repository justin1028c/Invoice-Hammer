package com.fordham.toolbelt.ui.tabs.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.BusinessStats
import com.fordham.toolbelt.ui.theme.StatsGreen
import com.fordham.toolbelt.util.DateTimeUtil
import invoicehammer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Responsibility: Layout orchestrator for the Bento-style statistics grid.
 */
@Composable
fun BentoGrid(stats: BusinessStats) {
    val animatedMargin = remember { Animatable(0f) }
    LaunchedEffect(stats.profitMargin) {
        animatedMargin.animateTo(
            targetValue = stats.profitMargin.toFloat(),
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Main Row: Health & Margin
        Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoCard(
                modifier = Modifier.weight(1.8f),
                title = stringResource(Res.string.net_profit_ytd),
                value = stats.formattedYtdNetProfit,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = StatsGreen,
                backgroundContent = {
                    TrendLineBackground(StatsGreen)
                }
            )
            BentoCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.margin_label),
                value = "${stats.profitMargin}%",
                icon = Icons.Default.PieChart,
                content = {
                    Box(modifier = Modifier.size(70.dp).padding(top = 4.dp), contentAlignment = Alignment.Center) {
                        DonutChartSmall(stats.netProfit, animatedMargin.value.toInt())
                        Text("${animatedMargin.value.toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }

        // Second Row: Hours & Unbilled
        Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val totalHours = stats.totalDurationSeconds / 3600.0
            BentoCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.total_hours_label),
                value = DateTimeUtil.formatDecimal(totalHours, 1),
                subValue = stringResource(Res.string.billable_label),
                icon = Icons.Default.Timer,
                color = MaterialTheme.colorScheme.primary,
                backgroundContent = {
                    TimePulseBackground(MaterialTheme.colorScheme.primary)
                }
            )
            BentoCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.unbilled),
                value = stats.formattedUnbilledExpenses,
                subValue = stringResource(Res.string.floating_costs_label),
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                color = MaterialTheme.colorScheme.secondary,
                backgroundContent = {
                    ReceiptDottedBackground(MaterialTheme.colorScheme.secondary)
                }
            )
        }
        
        // Third Row: Total Expenses & Tax Projection
        Row(modifier = Modifier.fillMaxWidth().height(175.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.total_business_expenses_label),
                value = DateTimeUtil.formatMoney(stats.totalExpenses),
                icon = Icons.Default.Payments,
                color = MaterialTheme.colorScheme.primary,
                backgroundContent = {
                    WavesBackground(MaterialTheme.colorScheme.primary)
                }
            )
            BentoCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.tax_projection),
                value = stats.formattedProjectedTax,
                icon = Icons.Default.Warning,
                color = Color(0xFFE67E22),
                content = {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = stats.formattedProjectedTax,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE67E22)
                        )
                        Text(
                            text = stringResource(Res.string.tax_warning, stats.currentQuarter),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 12.sp
                        )
                    }
                }
            )
        }

        // Fourth Row: Aged Receivables Card (Wide)
        BentoCard(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            title = stringResource(Res.string.aging_receivables),
            value = stats.formattedTotalOutstanding,
            icon = Icons.Default.Warning,
            isWide = true,
            color = Color(0xFFE74C3C),
            content = {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        text = stats.formattedTotalOutstanding,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SegmentedProgressBar(
                        outstanding0to30 = stats.outstanding0to30,
                        outstanding31to60 = stats.outstanding31to60,
                        outstanding61Plus = stats.outstanding61Plus
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(label = stringResource(Res.string.aging_0_30), amount = stats.formattedOutstanding0to30, color = Color(0xFF2ECC71))
                        LegendItem(label = stringResource(Res.string.aging_31_60), amount = stats.formattedOutstanding31to60, color = Color(0xFFF1C40F))
                        LegendItem(label = stringResource(Res.string.aging_61_plus), amount = stats.formattedOutstanding61Plus, color = Color(0xFFE74C3C))
                    }
                }
            }
        )
    }
}

@Composable
fun SegmentedProgressBar(
    outstanding0to30: Double,
    outstanding31to60: Double,
    outstanding61Plus: Double,
    modifier: Modifier = Modifier
) {
    val total = outstanding0to30 + outstanding31to60 + outstanding61Plus
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(outstanding0to30, outstanding31to60, outstanding61Plus) {
        animProgress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }

    if (total <= 0.0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(5.dp))
        )
    } else {
        val w0to30 = ((outstanding0to30 / total) * animProgress.value).toFloat()
        val w31to60 = ((outstanding31to60 / total) * animProgress.value).toFloat()
        val w61Plus = ((outstanding61Plus / total) * animProgress.value).toFloat()

        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color.Transparent)
        ) {
            if (w0to30 > 0f) {
                Box(
                    modifier = Modifier
                        .weight(w0to30)
                        .fillMaxHeight()
                        .background(
                            color = Color(0xFF2ECC71),
                            shape = when {
                                w31to60 == 0f && w61Plus == 0f -> RoundedCornerShape(5.dp)
                                else -> RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp)
                            }
                        )
                )
            }
            if (w31to60 > 0f) {
                if (w0to30 > 0f) Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .weight(w31to60)
                        .fillMaxHeight()
                        .background(
                            color = Color(0xFFF1C40F),
                            shape = when {
                                w0to30 == 0f && w61Plus == 0f -> RoundedCornerShape(5.dp)
                                w0to30 == 0f -> RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp)
                                w61Plus == 0f -> RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp)
                                else -> RoundedCornerShape(0.dp)
                            }
                        )
                )
            }
            if (w61Plus > 0f) {
                if (w0to30 > 0f || w31to60 > 0f) Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .weight(w61Plus)
                        .fillMaxHeight()
                        .background(
                            color = Color(0xFFE74C3C),
                            shape = when {
                                w0to30 == 0f && w31to60 == 0f -> RoundedCornerShape(5.dp)
                                else -> RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, amount: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
