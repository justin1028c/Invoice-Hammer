package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.viewmodel.ReceiptsUiState
import com.fordham.toolbelt.util.PlatformActions

@Composable
fun ReceiptsTab(
    uiState: ReceiptsUiState,
    selectedClient: Client?,
    allClients: List<Client>,
    filteredReceipts: List<ReceiptItem>,
    receiptsTotal: Double,
    totalWithMarkup: Double,
    onSetFilterClient: (String?) -> Unit,
    onSetClearConfirmVisible: (Boolean) -> Unit,
    onClearReceiptItems: () -> Unit,
    onReceiptUriSelected: (String) -> Unit,
    onSetClientDropdownVisible: (Boolean) -> Unit,
    onSelectClient: (Client?) -> Unit,
    onSetMarkupDialogVisible: (Boolean) -> Unit,
    onMarkupPercentageChange: (String) -> Unit,
    onProcessReceipt: () -> Unit,
    onClearCapturedReceipt: () -> Unit,
    onToggleReceiptBilled: (ReceiptItem) -> Unit,
    onDeleteReceiptItem: (ReceiptItem) -> Unit,
    platformActions: PlatformActions
) {
    LaunchedEffect(selectedClient) {
        onSetFilterClient(selectedClient?.name)
    }

    if (uiState.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { onSetClearConfirmVisible(false) },
            title = { Text("Clear All Receipts?", fontWeight = FontWeight.Black) },
            text = { Text("This will permanently delete all logged supplies and receipts. This action cannot be undone.") },
            confirmButton = { 
                TacticalButton(
                    onClick = { 
                        onClearReceiptItems()
                        onSetClearConfirmVisible(false)
                    }, 
                    text = "CLEAR ALL", 
                    containerColor = MaterialTheme.colorScheme.error
                ) 
            },
            dismissButton = { 
                TextButton(onClick = { onSetClearConfirmVisible(false) }) {
                    Text("CANCEL") 
                } 
            }
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "EXPENSE TRACKER", 
            style = MaterialTheme.typography.headlineSmall, 
            color = MaterialTheme.colorScheme.primary, 
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Text("Stop the leaks. Link receipts to specific jobs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            OutlinedButton(
                onClick = { onSetClientDropdownVisible(true) },
                modifier = Modifier.fillMaxWidth().height(48.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ComposeColor.Transparent
                )
            ) {
                Text(
                    (selectedClient?.name ?: "SELECT PROJECT / CLIENT").uppercase(), 
                    fontWeight = FontWeight.Black, 
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
            }
            DropdownMenu(
                expanded = uiState.showClientDropdown, 
                onDismissRequest = { onSetClientDropdownVisible(false) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                DropdownMenuItem(
                    text = { Text("GENERAL EXPENSES", fontWeight = FontWeight.Bold) }, 
                    onClick = { onSelectClient(null); onSetClientDropdownVisible(false) }
                )
                allClients.forEach { client ->
                    DropdownMenuItem(
                        text = { Text(client.name.uppercase(), fontWeight = FontWeight.Bold) }, 
                        onClick = { onSelectClient(client); onSetClientDropdownVisible(false) }
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TacticalButton(
                onClick = { 
                    platformActions.capturePhoto { uri ->
                        uri?.let { onReceiptUriSelected(it) }
                    }
                }, 
                text = "SNAP RECEIPT", 
                modifier = Modifier.weight(1f), 
                containerColor = MaterialTheme.colorScheme.primary,
                icon = { Icon(Icons.Default.CameraAlt, null) }
            )
            TacticalButton(
                onClick = { 
                    platformActions.pickImage { uri ->
                        uri?.let { onReceiptUriSelected(it) }
                    }
                }, 
                text = "UPLOAD", 
                modifier = Modifier.weight(1f), 
                containerColor = MaterialTheme.colorScheme.secondary, 
                icon = { Icon(Icons.Default.PhotoLibrary, null) }
            )
        }

        // Descriptive instructions card to utilize the empty space
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Instructions",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HOW TO TRACK EXPENSES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "1. Select a Client/Project from the dropdown above.\n2. Tap Snap Receipt or Upload to scan an expense image.\n3. Verify the captured image, click Scan & Log Receipt, and our AI engine will automatically extract line items and add them to your logged supplies below.",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Calculate, null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(16.dp))
            }
            TextButton(onClick = { onSetMarkupDialogVisible(true) }) {
                Text("MARKUP TOOL", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
            }
        }
        
        if (uiState.showMarkupDialog) {
            AlertDialog(
                onDismissRequest = { onSetMarkupDialogVisible(false) },
                title = { Text("MATERIAL MARKUP", fontWeight = FontWeight.Black) },
                text = {
                    Column {
                        Text("Add profit margin to your supplies.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.markupPercentage, 
                            onValueChange = { onMarkupPercentageChange(it) },
                            label = { Text("MARKUP %") }, 
                            modifier = Modifier.fillMaxWidth(), 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(4.dp),
                            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Black)
                        )
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("BILLED TOTAL:", fontWeight = FontWeight.Bold)
                                Text("$${totalWithMarkup}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                },
                confirmButton = { TacticalButton(onClick = { onSetMarkupDialogVisible(false) }, text = "APPLY") }
            )
        }

        if (uiState.capturedImageBytes != null) {
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    coil3.compose.AsyncImage(
                        model = uiState.capturedImageBytes,
                        contentDescription = "Captured Receipt",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = onClearCapturedReceipt,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(ComposeColor.Black.copy(alpha = 0.72f), RoundedCornerShape(999.dp))
                    ) {
                        Icon(Icons.Default.Close, "Remove receipt image", tint = ComposeColor.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        
        if (uiState.capturedImageBytes != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TacticalButton(
                    onClick = onProcessReceipt,
                    text = if (uiState.isProcessing) "ANALYZING..." else "SCAN & LOG RECEIPT",
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = { if (uiState.isProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Icon(Icons.Default.AutoAwesome, null) }
                )
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("LOGGED SUPPLIES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            TextButton(onClick = { onSetClearConfirmVisible(true) }) {
                Text("PURGE ALL", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredReceipts, key = { it.id.value }) { item ->
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
                                border = if (item.isBilled) null else FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
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
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TOTAL EXPENSES", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("$${totalWithMarkup}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
