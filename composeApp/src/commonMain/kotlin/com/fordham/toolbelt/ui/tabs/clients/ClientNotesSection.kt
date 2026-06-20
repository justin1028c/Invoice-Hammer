package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.JobNote
import com.fordham.toolbelt.ui.components.TacticalButton
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

/**
 * Responsibility: Display job notes, allow adding new ones, and show AI summaries.
 */
@Composable
fun ClientNotesSection(
    jobNotes: List<JobNote>,
    isPremium: Boolean,
    isSummarizing: Boolean,
    aiSummary: String?,
    onSummarizeClick: () -> Unit,
    onAddNoteClick: () -> Unit,
    onDeleteNoteClick: (JobNote) -> Unit,
    onClearAiSummary: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(Res.string.client_notes), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Row {
                if (jobNotes.isNotEmpty()) {
                    TextButton(onClick = onSummarizeClick, enabled = !isSummarizing && isPremium) {
                        if (isSummarizing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.ai_summary), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                TextButton(onClick = onAddNoteClick) {
                    Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.add_note), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary) 
                }
            }
        }
        
        aiSummary?.let { summaryText ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(Res.string.ai_job_insights), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = onClearAiSummary, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(summaryText.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            }
        }

        if (jobNotes.isEmpty()) {
            Text(stringResource(Res.string.no_notes_recorded), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
        } else {
            jobNotes.forEach { note ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(note.formattedDate.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                            IconButton(onClick = { onDeleteNoteClick(note) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) 
                            }
                        }
                        Text(note.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
