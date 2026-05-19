package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.Invoice

/**
 * Responsibility: Display a list of past invoices for the selected client.
 */
@Composable
fun ClientInvoicesSection(
    invoices: List<Invoice>,
    onInvoiceClick: (Invoice) -> Unit,
    onAddPhotoClick: (Invoice) -> Unit
) {
    Column {
        Text("PAST INVOICES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
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
                        Text(inv.date.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(inv.itemsSummary.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onAddPhotoClick(inv) }) {
                            Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Text(inv.formattedTotal, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                } 
            } 
        }
    }
}
