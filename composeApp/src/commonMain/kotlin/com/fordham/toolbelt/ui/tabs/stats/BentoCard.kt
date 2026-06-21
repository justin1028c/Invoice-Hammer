package com.fordham.toolbelt.ui.tabs.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
    onClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
    backgroundContent: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Card(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                } else Modifier
            ),
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
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
    }
    
    val infiniteTransition = rememberInfiniteTransition()
    val tracerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        val width = size.width
        val height = size.height
        
        path.moveTo(0f, height * 0.8f)
        path.quadraticTo(width * 0.2f, height * 0.85f, width * 0.4f, height * 0.6f)
        path.quadraticTo(width * 0.6f, height * 0.35f, width * 0.8f, height * 0.4f)
        path.lineTo(width, height * 0.1f)
        
        clipRect(right = progress.value * size.width) {
            drawPath(
                path = path,
                color = color.copy(alpha = 0.3f),
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
            
            val pathMeasure = PathMeasure()
            pathMeasure.setPath(path, false)
            val length = pathMeasure.length
            if (length > 0f) {
                val targetDistance = tracerProgress * length
                if (targetDistance <= progress.value * length) {
                    val pos = pathMeasure.getPosition(targetDistance)
                    
                    // Glow outer circle
                    drawCircle(
                        color = color.copy(alpha = 0.4f),
                        radius = 16f,
                        center = pos
                    )
                    // Bright core circle
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = pos
                    )
                }
            }
        }
    }
}

@Composable
fun TimePulseBackground(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseProgress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseProgress2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.8f, size.height * 0.4f)
        
        // Ring 1
        val radius1 = 40f + pulseProgress1 * 80f
        val alpha1 = (1f - pulseProgress1).coerceIn(0f, 1f) * 0.2f
        drawCircle(
            color = color.copy(alpha = alpha1),
            radius = radius1,
            center = center,
            style = Stroke(width = 4f)
        )
        
        // Ring 2
        val normProgress2 = if (pulseProgress2 > 1f) pulseProgress2 - 1f else pulseProgress2
        val radius2 = 40f + normProgress2 * 80f
        val alpha2 = (1f - normProgress2).coerceIn(0f, 1f) * 0.2f
        drawCircle(
            color = color.copy(alpha = alpha2),
            radius = radius2,
            center = center,
            style = Stroke(width = 4f)
        )
    }
}

@Composable
fun ReceiptDottedBackground(color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (i in 0..5) {
            for (j in 0..3) {
                drawCircle(
                    color = color.copy(alpha = 0.2f * progress.value),
                    radius = 3f,
                    center = Offset(size.width * 0.7f + (i * 15f), size.height * 0.2f + (j * 15f))
                )
            }
        }
    }
}

@Composable
fun WavesBackground(color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
    }
    
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * 3.14159265f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        clipRect(right = progress.value * size.width) {
            val path = Path()
            val w = size.width
            val h = size.height
            path.moveTo(0f, h * 0.7f)
            val steps = 40
            for (i in 1..steps) {
                val x = w * (i.toFloat() / steps)
                val relativeX = (i.toFloat() / steps) * 2f * 3.14159265f * 2f
                val y = h * 0.7f + kotlin.math.sin(relativeX + phase) * (h * 0.1f)
                path.lineTo(x, y)
            }
            drawPath(path, color.copy(alpha = 0.2f), style = Stroke(4f))
        }
    }
}
