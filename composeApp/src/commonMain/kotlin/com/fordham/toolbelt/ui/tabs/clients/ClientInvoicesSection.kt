package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.JobPhotoPhase
import com.fordham.toolbelt.util.DateTimeUtil
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

/**
 * Responsibility: Display an expandable list of past invoices for the selected client.
 */
@Composable
fun ClientInvoicesSection(
    invoices: List<Invoice>,
    onInvoiceClick: (Invoice) -> Unit,
    onEditAsDraftClick: (Invoice) -> Unit,
    onAddPhotoClick: (Invoice, JobPhotoPhase) -> Unit
) {
    var expandedInvoiceId by remember { mutableStateOf<String?>(null) }

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.past_invoices), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text("${invoices.size}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        if (invoices.isEmpty()) {
            Text(stringResource(Res.string.no_invoices_yet), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            return@Column
        }
        
        invoices.forEach { inv -> 
            val isExpanded = expandedInvoiceId == inv.id.value

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable {
                        expandedInvoiceId = if (isExpanded) null else inv.id.value
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (inv.isPaid) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) { 
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                InvoiceStatusPill(inv)
                                Text(DateTimeUtil.formatDateForDisplay(inv.date).uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            
                            if (!isExpanded) {
                                Text(
                                    text = inv.itemsSummary.value, 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    fontWeight = FontWeight.Bold, 
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    val items = remember(inv.itemsSummary.value) {
                                        inv.itemsSummary.value.split(Regex(",\\s*")).filter { it.isNotBlank() }
                                    }
                                    items.forEach { item ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "• ",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = item,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(inv.formattedTotal, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                            if (inv.depositAmount.value > 0.0) {
                                Text("DEP: ${inv.formattedDeposit}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    
                    if (isExpanded) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            if (inv.durationSeconds.value > 0L) {
                                val hrs = inv.durationSeconds.value / 3600
                                val mins = (inv.durationSeconds.value % 3600) / 60
                                Text(
                                    text = "Duration: ${hrs}h ${mins}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onInvoiceClick(inv) }, enabled = inv.pdfPath.value.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility, 
                                        contentDescription = "View PDF", 
                                        tint = if (inv.pdfPath.value.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray, 
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { onEditAsDraftClick(inv) }) {
                                    Icon(Icons.Default.Edit, "Edit as draft", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                }
                                var showPhotoMenu by remember(inv.id) { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { showPhotoMenu = true }) {
                                        Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    }
                                    DropdownMenu(expanded = showPhotoMenu, onDismissRequest = { showPhotoMenu = false }) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(Res.string.before), fontWeight = FontWeight.Black) },
                                            onClick = {
                                                showPhotoMenu = false
                                                onAddPhotoClick(inv, JobPhotoPhase.Before)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(Res.string.after), fontWeight = FontWeight.Black) },
                                            onClick = {
                                                showPhotoMenu = false
                                                onAddPhotoClick(inv, JobPhotoPhase.After)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } 
        }
    }
}

@Composable
private fun InvoiceStatusPill(invoice: Invoice) {
    val label = when {
        invoice.isEstimate -> "ESTIMATE"
        invoice.isPaid -> "PAID"
        else -> "UNPAID"
    }
    val color = when {
        invoice.isEstimate -> MaterialTheme.colorScheme.secondary
        invoice.isPaid -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            " $label ",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Black
        )
    }
}
