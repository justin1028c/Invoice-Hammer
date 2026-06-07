package com.fordham.toolbelt.ui.tabs.receipts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.ReceiptItem

@Composable
fun ReceiptsSupplyListSection(
    filteredReceipts: List<ReceiptItem>,
    totalWithMarkup: Double,
    onSetClearConfirmVisible: (Boolean) -> Unit,
    onToggleReceiptBilled: (ReceiptItem) -> Unit,
    onDeleteReceiptItem: (ReceiptItem) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "LOGGED SUPPLIES",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        TextButton(onClick = { onSetClearConfirmVisible(true) }) {
            Text("PURGE ALL", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
    }

    Column {
        filteredReceipts.forEach { item ->
            key(item.id.value) {
                ListItem(
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.description.uppercase(), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                            FilterChip(
                                selected = item.isBilled,
                                onClick = { onToggleReceiptBilled(item) },
                                label = { Text("BILLED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black) },
                                shape = RoundedCornerShape(2.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ComposeColor(0xFF00E676),
                                    selectedLabelColor = ComposeColor.Black
                                ),
                                border = if (item.isBilled) null else FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = false,
                                    borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                            )
                        }
                    },
                    supportingContent = { Text(item.formattedDetails, fontWeight = FontWeight.Bold) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.formattedPrice, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            IconButton(onClick = { onDeleteReceiptItem(item) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = ComposeColor.Transparent)
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TOTAL EXPENSES", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "$${totalWithMarkup}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
