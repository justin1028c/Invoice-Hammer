package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.usecase.ReminderChannel
import com.fordham.toolbelt.domain.usecase.ReminderTone
import com.fordham.toolbelt.ui.theme.BrandOrange
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiReminderSheet(
    invoice: Invoice,
    tone: ReminderTone,
    channel: ReminderChannel,
    generatedSubject: String,
    generatedBody: String,
    isGenerating: Boolean,
    error: String?,
    onToneChange: (ReminderTone) -> Unit,
    onChannelChange: (ReminderChannel) -> Unit,
    onGenerate: () -> Unit,
    onUpdateGeneratedText: (String, String) -> Unit,
    onSendShare: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .imePadding(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.reminder_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = BrandOrange
                    )
                }

                Text(
                    text = "${invoice.clientName.value.uppercase()} · ${invoice.formattedTotal}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandOrange
                )

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Tone Selection
                Text(
                    text = stringResource(Res.string.message_tone),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReminderTone.entries.forEach { t ->
                        val selected = t == tone
                        val label = when (t) {
                            ReminderTone.FRIENDLY -> stringResource(Res.string.tone_friendly)
                            ReminderTone.DIRECT -> stringResource(Res.string.tone_direct)
                            ReminderTone.FIRM -> stringResource(Res.string.tone_firm)
                        }
                        ElevatedFilterChip(
                            selected = selected,
                            onClick = { onToneChange(t) },
                            label = { Text(label, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Channel Selection
                Text(
                    text = stringResource(Res.string.message_channel),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReminderChannel.entries.forEach { c ->
                        val selected = c == channel
                        val label = when (c) {
                            ReminderChannel.SMS -> stringResource(Res.string.channel_sms)
                            ReminderChannel.EMAIL -> stringResource(Res.string.channel_email)
                        }
                        ElevatedFilterChip(
                            selected = selected,
                            onClick = { onChannelChange(c) },
                            label = { Text(label, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.secondary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Action: Generate
                Spacer(modifier = Modifier.height(4.dp))
                TacticalButton(
                    onClick = onGenerate,
                    text = stringResource(Res.string.generate_reminder).uppercase(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = { Icon(Icons.Default.AutoAwesome, null) },
                    enabled = !isGenerating
                )

                // Generating state
                if (isGenerating) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(Res.string.generating),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Error
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Results Fields
                if (generatedBody.isNotEmpty() && !isGenerating) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (channel == ReminderChannel.EMAIL) {
                            OutlinedTextField(
                                value = generatedSubject,
                                onValueChange = { onUpdateGeneratedText(it, generatedBody) },
                                label = { Text(stringResource(Res.string.reminder_subject)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Medium),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        OutlinedTextField(
                            value = generatedBody,
                            onValueChange = { onUpdateGeneratedText(generatedSubject, it) },
                            label = { Text(stringResource(Res.string.reminder_body)) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Medium),
                            maxLines = 10,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TacticalButton(
                        onClick = onSendShare,
                        text = stringResource(Res.string.send_reminder).uppercase(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        icon = { Icon(Icons.AutoMirrored.Filled.Send, null) }
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stringResource(Res.string.close),
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
