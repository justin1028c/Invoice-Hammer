package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun TacticalButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White, // Defaulting to White for contrast against BrandOrange
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "press_scale"
    )

    // Contextual border: glowing in dark mode, sharp black in light mode
    val isDarkMode = MaterialTheme.colorScheme.background == Color(0xFF000000)
    val borderColor = if (isDarkMode) containerColor else Color.Black
    val borderWidth = if (isDarkMode) 1.dp else 2.dp

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        contentPadding = contentPadding,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text.uppercase(), // Force uppercase for tactical feel
                    fontWeight = FontWeight.Black, 
                    fontSize = fontSize, 
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
