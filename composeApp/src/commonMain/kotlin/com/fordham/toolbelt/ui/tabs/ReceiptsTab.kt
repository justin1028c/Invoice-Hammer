package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.tabs.receipts.ReceiptsCaptureSection
import com.fordham.toolbelt.ui.tabs.receipts.ReceiptsSupplyListSection
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

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            "EXPENSE TRACKER",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Text(
            "Stop the leaks. Link receipts to specific jobs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ReceiptsCaptureSection(
            uiState = uiState,
            selectedClient = selectedClient,
            allClients = allClients,
            totalWithMarkup = totalWithMarkup,
            onSetClientDropdownVisible = onSetClientDropdownVisible,
            onSelectClient = onSelectClient,
            onSetMarkupDialogVisible = onSetMarkupDialogVisible,
            onMarkupPercentageChange = onMarkupPercentageChange,
            onReceiptUriSelected = onReceiptUriSelected,
            onProcessReceipt = onProcessReceipt,
            onClearCapturedReceipt = onClearCapturedReceipt,
            platformActions = platformActions
        )

        ReceiptsSupplyListSection(
            filteredReceipts = filteredReceipts,
            totalWithMarkup = totalWithMarkup,
            onSetClearConfirmVisible = onSetClearConfirmVisible,
            onToggleReceiptBilled = onToggleReceiptBilled,
            onDeleteReceiptItem = onDeleteReceiptItem
        )
    }
}
