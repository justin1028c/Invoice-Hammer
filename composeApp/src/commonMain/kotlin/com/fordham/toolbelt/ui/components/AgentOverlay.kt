package com.fordham.toolbelt.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
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
import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.ClientSearchHit
import com.fordham.toolbelt.domain.model.agent.ForemanAgentPresentation
import com.fordham.toolbelt.domain.model.agent.InvoiceSavePreview

@Composable
fun AgentOverlay(
    isActive: Boolean,
    isProcessing: Boolean,
    currentMode: AgentMode,
    transcript: String,
    lastResponse: String?,
    isListening: Boolean,
    typedCommand: String,
    onTypedCommandChange: (String) -> Unit,
    onSendTypedCommand: () -> Unit,
    onDismiss: () -> Unit,
    onMicClick: () -> Unit,
    onApprove: () -> Unit,
    pendingApproval: AgentOutcome.RequiresApproval? = null,
    clientChoices: List<ClientSearchHit> = emptyList(),
    onSelectClient: (ClientId) -> Unit = {},
    onDismissClientChoices: () -> Unit = {},
    savePreview: InvoiceSavePreview? = null,
    onConfirmSave: () -> Unit = {},
    onDismissSavePreview: () -> Unit = {},
    stepSummaries: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (!isActive) return

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
            .widthIn(max = 400.dp)
            .then(
                when {
                    isListening -> Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                        RoundedCornerShape(12.dp)
                    )
                    pendingApproval != null -> Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.error,
                        RoundedCornerShape(12.dp)
                    )
                    clientChoices.isNotEmpty() -> Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.tertiary,
                        RoundedCornerShape(12.dp)
                    )
                    else -> Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                pendingApproval != null -> MaterialTheme.colorScheme.errorContainer
                clientChoices.isNotEmpty() -> MaterialTheme.colorScheme.tertiaryContainer
                currentMode == AgentMode.ACTION -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AgentOverlayHeader(
                isListening = isListening,
                isProcessing = isProcessing,
                pendingApproval = pendingApproval,
                clientChoices = clientChoices,
                currentMode = currentMode,
                alpha = alpha,
                onMicClick = onMicClick,
                onDismiss = onDismiss,
                showMic = !isProcessing && pendingApproval == null && clientChoices.isEmpty() && savePreview == null
            )

            Spacer(Modifier.height(8.dp))

            when {
                isProcessing -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Working on: \"$transcript\"", style = MaterialTheme.typography.bodySmall)
                }
                clientChoices.isNotEmpty() -> {
                    Text("Which client?", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        clientChoices.forEach { hit ->
                            FilterChip(
                                selected = false,
                                onClick = { onSelectClient(hit.clientId) },
                                label = { Text(hit.displayName.value) }
                            )
                        }
                    }
                    TextButton(onClick = onDismissClientChoices) { Text("Cancel") }
                }
                savePreview != null -> {
                    Text("Save invoice?", fontWeight = FontWeight.Black)
                    Text(
                        "${savePreview.clientName.value} · ${savePreview.lineItemCount} line(s) · " +
                            "~$${(savePreview.estimatedTotal * 100).toInt() / 100.0}" +
                            if (savePreview.isEstimate) " (estimate)" else "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onDismissSavePreview) { Text("Edit draft") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onConfirmSave) { Text("Save PDF") }
                    }
                }
                pendingApproval != null -> {
                    AgentApprovalCard(pendingApproval = pendingApproval, onDismiss = onDismiss, onApprove = onApprove)
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            lastResponse ?: if (isListening) "Speak now..." else "Ready.",
                            fontWeight = FontWeight.Bold
                        )
                        if (stepSummaries.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            stepSummaries.forEach { step ->
                                Text(
                                    "• $step",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (!isListening) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = typedCommand,
                            onValueChange = onTypedCommandChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Type a command…") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = onSendTypedCommand,
                                    enabled = typedCommand.isNotBlank() && !isProcessing
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send")
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "QUICK WORKFLOWS (TAP TO FILL)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(4.dp))
                        val suggestions = listOf(
                            "Invoice Bob 3h labor at 85",
                            "Go to stats",
                            "Go to history",
                            "Bill Joe 500",
                            "Same as last time for Mike"
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            suggestions.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = { onTypedCommandChange(suggestion) },
                                    label = { Text(suggestion, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentOverlayHeader(
    isListening: Boolean,
    isProcessing: Boolean,
    pendingApproval: AgentOutcome.RequiresApproval?,
    clientChoices: List<ClientSearchHit>,
    currentMode: AgentMode,
    alpha: Float,
    onMicClick: () -> Unit,
    onDismiss: () -> Unit,
    showMic: Boolean
) {
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
                        when {
                            isListening -> MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                            pendingApproval != null -> MaterialTheme.colorScheme.error
                            clientChoices.isNotEmpty() -> MaterialTheme.colorScheme.tertiary
                            else -> Color.Transparent
                        },
                        CircleShape
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    clientChoices.isNotEmpty() -> "PICK CLIENT"
                    pendingApproval != null -> "APPROVAL NEEDED"
                    isListening -> "LISTENING"
                    isProcessing -> "WORKING"
                    currentMode == AgentMode.ACTION -> "AI ACTION"
                    else -> "FOREMAN"
                },
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showMic) {
                IconButton(onClick = onMicClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    }
}

@Composable
private fun AgentApprovalCard(
    pendingApproval: AgentOutcome.RequiresApproval,
    onDismiss: () -> Unit,
    onApprove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                ForemanAgentPresentation.approvalMessage(pendingApproval),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) { Text("REJECT") }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onApprove,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("APPROVE", fontWeight = FontWeight.Bold)
        }
    }
}
