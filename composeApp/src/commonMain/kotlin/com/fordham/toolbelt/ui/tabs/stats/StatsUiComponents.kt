package com.fordham.toolbelt.ui.tabs.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
        
        if (sweepAngle > 0f) {
            val strokeWidth = 15f
            val radius = (size.minDimension - strokeWidth) / 2f
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            
            // Starts at -90 degrees
            val endAngleDegrees = -90f + sweepAngle
            val angleRad = endAngleDegrees * (3.14159265f / 180f)
            
            val tracerPos = Offset(
                x = centerOffset.x + radius * kotlin.math.cos(angleRad),
                y = centerOffset.y + radius * kotlin.math.sin(angleRad)
            )
            
            // Draw a glowing tip indicator
            drawCircle(
                color = arcColor.copy(alpha = 0.4f),
                radius = 12f,
                center = tracerPos
            )
            drawCircle(
                color = Color.White,
                radius = 5f,
                center = tracerPos
            )
        }
    }
}
