package com.fordham.toolbelt.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.AgentMode

@Composable
fun AgentOverlay(
    isActive: Boolean,
    isProcessing: Boolean,
    currentMode: AgentMode,
    transcript: String,
    lastResponse: String?,
    isListening: Boolean,
    onDismiss: () -> Unit,
    onMicClick: () -> Unit,
    onApprove: () -> Unit,
    pendingApproval: com.fordham.toolbelt.domain.model.ForemanToolCall? = null,
    modifier: Modifier = Modifier
) {
    if (!isProcessing && !isListening && pendingApproval == null && lastResponse == null) return

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .padding(start = 12.dp, end = 96.dp, bottom = 8.dp)
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .then(
                if (isListening) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    RoundedCornerShape(12.dp)
                ) else if (pendingApproval != null) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.error,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (pendingApproval != null) 
                MaterialTheme.colorScheme.errorContainer 
            else if (currentMode == AgentMode.ACTION)
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isListening) MaterialTheme.colorScheme.primary.copy(alpha = alpha) 
                                else if (pendingApproval != null) MaterialTheme.colorScheme.error
                                else Color.Transparent, 
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            pendingApproval != null -> "APPROVAL NEEDED"
                            isListening -> "LISTENING"
                            isProcessing -> "WORKING"
                            currentMode == AgentMode.ACTION -> "AI ACTION"
                            else -> "AI RESPONSE"
                        },
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pendingApproval != null) MaterialTheme.colorScheme.error
                        else if (currentMode == AgentMode.ACTION) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.secondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isProcessing && pendingApproval == null) {
                        IconButton(onClick = onMicClick, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Speak",
                                tint = if (isListening) MaterialTheme.colorScheme.primary.copy(alpha = alpha) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isListening) 24.dp else 20.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            if (isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Checking: \"${transcript}\"", style = MaterialTheme.typography.bodySmall)
            } else {
                if (pendingApproval != null) {
                    Text("The agent wants to: ${pendingApproval.type.name}", fontWeight = FontWeight.Bold)
                    Text(pendingApproval.reasoning, style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("REJECT") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("APPROVE ACTION")
                        }
                    }
                } else {
                    Text(
                        lastResponse ?: if (isListening) "Speak now..." else "Ready.",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
