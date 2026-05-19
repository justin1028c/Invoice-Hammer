package com.fordham.toolbelt.ui.tabs.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.ProjectStat
import com.fordham.toolbelt.ui.theme.StatsGreen

/**
 * Responsibility: Display profitability and progress for a specific project.
 */
@Composable
fun ProjectStatCard(project: ProjectStat) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        border = if (project.profit < 0) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
                Text(project.clientName, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(project.formattedProfit, color = if (project.profit >= 0) StatsGreen else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium) 
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { project.progress }, 
                modifier = Modifier.fillMaxWidth().height(6.dp), 
                color = if (project.profit >= 0) StatsGreen else MaterialTheme.colorScheme.error, 
                strokeCap = StrokeCap.Round,
                trackColor = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                Text(project.formattedRevenue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                Text(project.formattedExpenses, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}
