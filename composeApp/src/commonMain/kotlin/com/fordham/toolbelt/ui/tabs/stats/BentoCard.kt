package com.fordham.toolbelt.ui.tabs.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Responsibility: Versatile card container for the Bento grid with support for generative backgrounds.
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String? = null,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    gradient: List<Color>? = null,
    isWide: Boolean = false,
    content: (@Composable () -> Unit)? = null,
    backgroundContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (gradient != null) Modifier.background(Brush.verticalGradient(gradient))
                    else Modifier
                )
        ) {
            backgroundContent?.invoke()

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).graphicsLayer(alpha = 0.5f),
                    tint = color
                )
                
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        letterSpacing = 1.sp
                    )
                    if (content != null) {
                        content()
                    } else {
                        Text(
                            text = value,
                            style = if (isWide) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = color
                        )
                    }
                    if (subValue != null) {
                        Text(
                            text = subValue,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrendLineBackground(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        val width = size.width
        val height = size.height
        
        path.moveTo(0f, height * 0.8f)
        path.quadraticTo(width * 0.2f, height * 0.85f, width * 0.4f, height * 0.6f)
        path.quadraticTo(width * 0.6f, height * 0.35f, width * 0.8f, height * 0.4f)
        path.lineTo(width, height * 0.1f)
        
        drawPath(
            path = path,
            color = color.copy(alpha = 0.3f),
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun TimePulseBackground(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.8f, size.height * 0.4f)
        drawCircle(
            color = color.copy(alpha = 0.15f),
            radius = 60f,
            center = center,
            style = Stroke(width = 4f)
        )
        drawCircle(
            color = color.copy(alpha = 0.1f),
            radius = 100f,
            center = center,
            style = Stroke(width = 2f)
        )
    }
}

@Composable
fun ReceiptDottedBackground(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (i in 0..5) {
            for (j in 0..3) {
                drawCircle(
                    color = color.copy(alpha = 0.2f),
                    radius = 3f,
                    center = Offset(size.width * 0.7f + (i * 15f), size.height * 0.2f + (j * 15f))
                )
            }
        }
    }
}

@Composable
fun WavesBackground(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        val w = size.width
        val h = size.height
        path.moveTo(0f, h * 0.7f)
        for (i in 1..4) {
            val x = w * (i / 4f)
            val y = if (i % 2 == 0) h * 0.6f else h * 0.8f
            path.lineTo(x, y)
        }
        drawPath(path, color.copy(alpha = 0.2f), style = Stroke(4f))
    }
}
