package com.fordham.toolbelt.ui.tabs.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceUiState
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.ItemsSummary
import com.fordham.toolbelt.domain.model.LineItem
import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.localizeInvoiceCategory
import com.fordham.toolbelt.util.DateTimeUtil
import invoicehammer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun InvoiceLineItemsList(
    uiState: NewInvoiceUiState,
    businessSettings: BusinessSettings,
    categories: List<String>,
    onSetReceiptPickerVisible: (Boolean) -> Unit,
    onSetInvoiceCategoryDropdownVisible: (Boolean) -> Unit,
    onCategoryChange: (String) -> Unit,
    onItemDescChange: (String) -> Unit,
    onItemAmtChange: (String) -> Unit,
    onProcessInvoiceAi: (List<String>) -> Unit,
    onAddManualLineItem: () -> Unit,
    onRemoveLineItem: (LineItem) -> Unit,
    onUpdateLineItem: (LineItem, LineItem) -> Unit
) {
    val lineItemsTitle = stringResource(Res.string.line_items)
    val linkUnbilledText = stringResource(Res.string.link_unbilled)
    val descriptionLabel = stringResource(Res.string.description_label)
    val priceLabel = stringResource(Res.string.price_label)
    val aiFillText = stringResource(Res.string.ai_fill)
    val addItemText = stringResource(Res.string.add_item)
    val lineItemsEmptyText = stringResource(Res.string.line_items_empty)
    var editingItem by remember { mutableStateOf<LineItem?>(null) }
    var editDescription by remember { mutableStateOf("") }
    var editAmount by remember { mutableStateOf("") }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(lineItemsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
            if (uiState.availableReceipts.isNotEmpty()) {
                TextButton(onClick = { onSetReceiptPickerVisible(true) }) {
                    Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(linkUnbilledText, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(
                        onClick = { onSetInvoiceCategoryDropdownVisible(true) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(localizeInvoiceCategory(uiState.selectedCategory).uppercase(), fontWeight = FontWeight.Black)
                    }
                    DropdownMenu(
                        expanded = uiState.showCategoryDropdown,
                        onDismissRequest = { onSetInvoiceCategoryDropdownVisible(false) }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(localizeInvoiceCategory(category).uppercase(), fontWeight = FontWeight.Bold) },
                                onClick = { 
                                    onCategoryChange(category)
                                    onSetInvoiceCategoryDropdownVisible(false)
                                }
                            ) 
                        }
                    }
                }
                OutlinedTextField(
                    value = uiState.itemDesc,
                    onValueChange = { onItemDescChange(it) },
                    label = { Text(descriptionLabel, fontWeight = FontWeight.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = uiState.itemAmt,
                        onValueChange = { onItemAmtChange(it) },
                        label = { Text(priceLabel, fontWeight = FontWeight.Black) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TacticalButton(
                            onClick = { onProcessInvoiceAi(categories) },
                            text = if (uiState.isProcessingAi) "" else aiFillText,
                            enabled = !uiState.isProcessingAi && uiState.itemDesc.length > 5 && businessSettings.isPremium,
                            containerColor = MaterialTheme.colorScheme.primary,
                            icon = { if (uiState.isProcessingAi) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Default.AutoAwesome, null) }
                        )
                    }
                }
                TacticalButton(onClick = { onAddManualLineItem() }, text = addItemText, modifier = Modifier.align(Alignment.End), containerColor = MaterialTheme.colorScheme.secondary, enabled = uiState.canAddManual)
            }
        }
        uiState.lineItems.forEach { item ->
            ListItem(
                headlineContent = { Text(item.category + ": $" + DateTimeUtil.formatDecimal(item.amount.value, 2), fontWeight = FontWeight.Bold) },
                supportingContent = { Text(item.description.value) },
                trailingContent = {
                    Row {
                        IconButton(
                            onClick = {
                                editingItem = item
                                editDescription = item.description.value
                                editAmount = DateTimeUtil.formatDecimal(item.amount.value, 2)
                            }
                        ) {
                            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onRemoveLineItem(item) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
        if (uiState.lineItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(lineItemsEmptyText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
            }
        }
    }

    editingItem?.let { item ->
        val amount = editAmount.toDoubleOrNull()
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text(stringResource(Res.string.edit_line_item), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text(descriptionLabel, fontWeight = FontWeight.Black) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp)
                    )
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text(priceLabel, fontWeight = FontWeight.Black) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            },
            confirmButton = {
                TacticalButton(
                    onClick = {
                        val updated = item.copy(
                            description = ItemsSummary(editDescription.trim()),
                            amount = MoneyAmount(amount ?: item.amount.value)
                        )
                        onUpdateLineItem(item, updated)
                        editingItem = null
                    },
                    text = stringResource(Res.string.save),
                    enabled = editDescription.isNotBlank() && amount != null && amount >= 0.0,
                    containerColor = MaterialTheme.colorScheme.primary
                )
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}
