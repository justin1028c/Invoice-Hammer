package com.fordham.toolbelt.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import invoicehammer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun MainAgentFab(
    isListening: Boolean,
    isPremium: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onPremiumRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listeningText = stringResource(Res.string.listening)
    val askAiText = stringResource(Res.string.ask_ai)
    val stopGeminiCd = stringResource(Res.string.stop_gemini_ai_cd)
    val askGeminiCd = stringResource(Res.string.ask_gemini_ai_cd)

    val infiniteTransition = rememberInfiniteTransition(label = "geminiFabGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.padding(bottom = 22.dp, end = 2.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(999.dp),
            tonalElevation = 3.dp,
            shadowElevation = 2.dp
        ) {
            Text(
                text = if (isListening) listeningText else askAiText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        FloatingActionButton(
            onClick = {
                if (!isPremium) {
                    onPremiumRequired()
                } else if (isListening) {
                    onStopListening()
                } else {
                    onStartListening()
                }
            },
            containerColor = if (isListening) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
            contentColor = if (isListening) Color.Black else MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    if (isListening) {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.AutoAwesome,
                    contentDescription = if (isListening) stopGeminiCd else askGeminiCd,
                    modifier = Modifier.size(28.dp)
                )
                if (!isPremium) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}
