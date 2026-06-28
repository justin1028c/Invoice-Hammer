package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onAcceptSingleExpenseMatch: (ReceiptItem) -> Unit,
    onDeclineExpenseMatch: () -> Unit,
    platformActions: PlatformActions
) {
    LaunchedEffect(selectedClient) {
        onSetFilterClient(selectedClient?.name?.value)
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
        AlertDialog(
            onDismissRequest = onDeclineExpenseMatch,
            title = {
                Text(
                    text = "Review Scanned Items",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Match found for ${match.clientName} (${match.category}). Choose items to add:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        uiState.processedItems.forEach { item ->
                            key(item.id.value) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${item.quantity}x @ ${com.fordham.toolbelt.util.DateTimeUtil.formatMoney(item.unitPrice ?: 0.0)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    if (item.isBilled) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Added",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Added",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        val markup = uiState.markupPercentage.toDoubleOrNull() ?: 0.0
                                        val priceText = if (markup > 0.0) {
                                            val billedPrice = item.totalPrice * (1.0 + (markup / 100.0))
                                            "+$${com.fordham.toolbelt.util.DateTimeUtil.formatDecimal(billedPrice, 2)}"
                                        } else {
                                            "+$${com.fordham.toolbelt.util.DateTimeUtil.formatDecimal(item.totalPrice, 2)}"
                                        }
                                        
                                        Button(
                                            onClick = { onAcceptSingleExpenseMatch(item) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(
                                                text = priceText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TacticalButton(
                    onClick = onDeclineExpenseMatch,
                    text = "Done",
                    containerColor = MaterialTheme.colorScheme.secondary
                )
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
