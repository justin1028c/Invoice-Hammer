package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.usecase.FinancialSummary
import com.fordham.toolbelt.ui.theme.StatsGreen
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

/**
 * Responsibility: Display a summary of revenue, costs, and profit for a specific client.
 */
@Composable
fun ClientFinancialSummaryCard(
    summary: FinancialSummary,
    hasAvailableReceipts: Boolean,
    onLinkReceiptClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), 
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(Res.string.job_summary), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                Text(stringResource(Res.string.revenue_label), fontWeight = FontWeight.Bold)
                Text(summary.formattedRevenue, fontWeight = FontWeight.Black) 
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
                Text(stringResource(Res.string.costs_label), fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(summary.formattedExpenses, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                    if (hasAvailableReceipts) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onLinkReceiptClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Link, stringResource(Res.string.link_receipt), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                Text(stringResource(Res.string.net_profit_label), fontWeight = FontWeight.Black)
                Text(summary.formattedProfit, fontWeight = FontWeight.Black, color = if (summary.profit >= 0) StatsGreen else MaterialTheme.colorScheme.error) 
            }
        }
    }
}
