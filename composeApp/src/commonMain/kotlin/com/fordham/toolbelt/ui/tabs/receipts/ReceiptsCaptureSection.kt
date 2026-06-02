package com.fordham.toolbelt.ui.tabs.receipts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.viewmodel.ReceiptsUiState
import com.fordham.toolbelt.util.PlatformActions

@Composable
fun ReceiptsCaptureSection(
    uiState: ReceiptsUiState,
    selectedClient: Client?,
    allClients: List<Client>,
    totalWithMarkup: Double,
    onSetClientDropdownVisible: (Boolean) -> Unit,
    onSelectClient: (Client?) -> Unit,
    onSetMarkupDialogVisible: (Boolean) -> Unit,
    onMarkupPercentageChange: (String) -> Unit,
    onReceiptUriSelected: (String) -> Unit,
    onProcessReceipt: () -> Unit,
    onClearCapturedReceipt: () -> Unit,
    platformActions: PlatformActions
) {
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
                        onValueChange = onMarkupPercentageChange,
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

    uiState.errorMessage?.let { message ->
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        OutlinedButton(
            onClick = { onSetClientDropdownVisible(true) },
            modifier = Modifier.fillMaxWidth().height(48.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = ComposeColor.Transparent)
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
                platformActions.capturePhoto { uri -> uri?.let { onReceiptUriSelected(it) } }
            },
            text = "SNAP RECEIPT",
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.primary,
            icon = { Icon(Icons.Default.CameraAlt, null) }
        )
        TacticalButton(
            onClick = {
                platformActions.pickImage { uri -> uri?.let { onReceiptUriSelected(it) } }
            },
            text = "UPLOAD",
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.secondary,
            icon = { Icon(Icons.Default.PhotoLibrary, null) }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, "Instructions", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "HOW TO TRACK EXPENSES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "1. Select a Client/Project from the dropdown above.\n2. Tap Snap Receipt or Upload to scan an expense image.\n3. Verify the captured image, click Scan & Log Receipt, and our AI engine will automatically extract line items and add them to your logged supplies below.",
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TacticalButton(
                onClick = onProcessReceipt,
                text = if (uiState.isProcessing) "ANALYZING..." else "SCAN & LOG RECEIPT",
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primary,
                icon = {
                    if (uiState.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.AutoAwesome, null)
                    }
                }
            )
        }
    }
}
