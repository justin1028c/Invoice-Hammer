package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
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
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.JobPhotoPhase
import com.fordham.toolbelt.util.DateTimeUtil
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

/**
 * Responsibility: Display a list of past invoices for the selected client.
 */
@Composable
fun ClientInvoicesSection(
    invoices: List<Invoice>,
    onInvoiceClick: (Invoice) -> Unit,
    onAddPhotoClick: (Invoice, JobPhotoPhase) -> Unit
) {
    Column {
        Text(stringResource(Res.string.past_invoices), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        
        invoices.forEach { inv -> 
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onInvoiceClick(inv) }, 
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) { 
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) { 
                    Column(modifier = Modifier.weight(1f)) { 
                        Text(DateTimeUtil.formatDateForDisplay(inv.date).uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(inv.itemsSummary.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var showPhotoMenu by remember(inv.id) { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showPhotoMenu = true }) {
                                Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
                        Text(inv.formattedTotal, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                } 
            } 
        }
    }
}
