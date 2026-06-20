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
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

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
    onAcceptExpenseMatch: () -> Unit,
    onDeclineExpenseMatch: () -> Unit,
    platformActions: PlatformActions
) {
    LaunchedEffect(selectedClient) {
        onSetFilterClient(selectedClient?.name)
    }

    if (uiState.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { onSetClearConfirmVisible(false) },
            title = { Text(stringResource(Res.string.clear_all_receipts), fontWeight = FontWeight.Black) },
            text = { Text(stringResource(Res.string.clear_all_receipts_desc)) },
            confirmButton = {
                TacticalButton(
                    onClick = {
                        onClearReceiptItems()
                        onSetClearConfirmVisible(false)
                    },
                    text = stringResource(Res.string.purge_all),
                    containerColor = MaterialTheme.colorScheme.error
                )
            },
            dismissButton = {
                TextButton(onClick = { onSetClearConfirmVisible(false) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (uiState.pendingMatch != null) {
        val match = uiState.pendingMatch
        val formatAmount = com.fordham.toolbelt.util.DateTimeUtil.formatMoney(match.totalAmount)
        AlertDialog(
            onDismissRequest = onDeclineExpenseMatch,
            title = { Text(stringResource(Res.string.match_expense_title), fontWeight = FontWeight.Black) },
            text = {
                Text(
                    stringResource(
                        Res.string.match_expense_desc,
                        match.clientName,
                        match.category,
                        formatAmount
                    )
                )
            },
            confirmButton = {
                TacticalButton(
                    onClick = onAcceptExpenseMatch,
                    text = stringResource(Res.string.append_to_invoice),
                    containerColor = MaterialTheme.colorScheme.primary
                )
            },
            dismissButton = {
                TextButton(onClick = onDeclineExpenseMatch) {
                    Text(stringResource(Res.string.no_thanks))
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            stringResource(Res.string.expense_tracker),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Text(
            stringResource(Res.string.expense_tracker_desc),
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
