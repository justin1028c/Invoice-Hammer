package com.fordham.toolbelt.ui.tabs.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import com.fordham.toolbelt.ui.theme.StatsGreen

/**
 * Responsibility: Specialized chart components for the Stats tab.
 */
@Composable
fun DonutChartSmall(profit: Double, margin: Int) {
    val sweepAngle = (margin.toFloat() / 100f * 360f).coerceIn(0f, 360f)
    val arcColor = if (profit >= 0) StatsGreen else MaterialTheme.colorScheme.error
    
    val trackColor = MaterialTheme.colorScheme.outline
    Canvas(modifier = Modifier.fillMaxSize()) { 
        drawArc(trackColor, 0f, 360f, false, style = Stroke(15f))
        drawArc(arcColor, -90f, sweepAngle, false, style = Stroke(15f)) 
    }
}
