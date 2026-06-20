package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.ui.components.CapturedJobPhotoStrip
import com.fordham.toolbelt.ui.components.JobPhotoCaptureButtons
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.tabs.components.*
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceUiState
import com.fordham.toolbelt.util.Permission
import com.fordham.toolbelt.util.PlatformActions
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

@Composable
fun NewInvoiceTab(
    uiState: NewInvoiceUiState,
    businessSettings: BusinessSettings,
    allClients: List<Client>,
    categories: List<String>,
    onSaveBusinessSettings: (BusinessSettings) -> Unit,
    onTimerToggle: () -> Unit,
    onHourlyRateChange: (String) -> Unit,
    onBillLabor: () -> Unit,
    onLogoUriChange: (String?) -> Unit,
    onPhotoCaptured: (String, JobPhotoPhase) -> Unit,
    onClientNameChange: (String) -> Unit,
    onClientAddressChange: (String) -> Unit,
    onSetInvoiceClientDropdownVisible: (Boolean) -> Unit,
    onSaveToClientDirectoryChange: (Boolean) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onSetReceiptPickerVisible: (Boolean) -> Unit,
    onSetInvoiceCategoryDropdownVisible: (Boolean) -> Unit,
    onCategoryChange: (String) -> Unit,
    onItemDescChange: (String) -> Unit,
    onItemAmtChange: (String) -> Unit,
    onProcessInvoiceAi: (List<String>) -> Unit,
    onAddManualLineItem: () -> Unit,
    onRemoveLineItem: (LineItem) -> Unit,
    onTaxTextChange: (String) -> Unit,
    onDepositCollectedChange: (String) -> Unit,
    onSaveInvoice: (Boolean, BusinessSettings, (String) -> Unit) -> Unit,
    onLinkReceipt: (ReceiptItem, Double) -> Unit,
    onShareFile: (String, String) -> Unit,
    platformActions: PlatformActions,
    isPremium: Boolean = false
) {
    var showBusinessDialog by remember { mutableStateOf(false) }

    if (showBusinessDialog) {
        BusinessProfileDialog(
            businessSettings = businessSettings,
            onDismiss = { showBusinessDialog = false },
            onSave = { onSaveBusinessSettings(it) },
            onPickLogo = {
                platformActions.pickImage { uri ->
                    uri?.let { onLogoUriChange(it) }
                }
            },
            onRemoveLogo = { onLogoUriChange(null) }
        )
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.invoice_details), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            TextButton(onClick = { showBusinessDialog = true }) {
                Icon(Icons.Default.Business, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.business), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        }
        
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(2.dp, if (uiState.timerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(stringResource(Res.string.job_timer), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                        Text(uiState.formattedTime, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    TacticalButton(
                        onClick = { onTimerToggle() }, 
                        text = if (!uiState.timerRunning) stringResource(Res.string.start_timer) else stringResource(Res.string.stop_timer), 
                        icon = { Icon(if (!uiState.timerRunning) Icons.Default.PlayArrow else Icons.Default.Stop, null) },
                        containerColor = if (!uiState.timerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                if (!uiState.timerRunning && uiState.elapsedSeconds > 0) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = uiState.hourlyRate,
                            onValueChange = { onHourlyRateChange(it) },
                            label = { Text(stringResource(Res.string.rate_hr), fontWeight = FontWeight.Black) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        TacticalButton(onClick = { onBillLabor() }, text = stringResource(Res.string.bill_time))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        com.fordham.toolbelt.ui.components.BusinessLogoSection(
            logoUri = uiState.logoUri,
            onPickLogo = {
                platformActions.pickImage { uri ->
                    uri?.let { onLogoUriChange(it) }
                }
            },
            onRemoveLogo = if (uiState.logoUri != null) {
                { onLogoUriChange(null) }
            } else {
                null
            }
        )
        if (uiState.businessLogoSaved && uiState.logoUri != null) {
            Text(
                stringResource(Res.string.saved_future),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(10.dp))
        Box {
            OutlinedTextField(
                value = uiState.clientName,
                onValueChange = { onClientNameChange(it) },
                label = { Text(stringResource(Res.string.client_name), fontWeight = FontWeight.Black) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                trailingIcon = { IconButton(onClick = { onSetInvoiceClientDropdownVisible(true) }) { Icon(Icons.Default.ArrowDropDown, null) } }
            )
            DropdownMenu(expanded = uiState.showClientDropdown, onDismissRequest = { onSetInvoiceClientDropdownVisible(false) }) {
                allClients.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.name.uppercase(), fontWeight = FontWeight.Bold) },
                        onClick = { 
                            onClientNameChange(c.name)
                            onClientAddressChange(c.address)
                            onSetInvoiceClientDropdownVisible(false)
                        }
                    )
                }
            }
        }
        OutlinedTextField(value = uiState.clientAddress, onValueChange = { onClientAddressChange(it) }, label = { Text(stringResource(Res.string.client_address), fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = uiState.saveToClientDirectory, onCheckedChange = { onSaveToClientDirectoryChange(it) }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
            Text(stringResource(Res.string.save_client_dir), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(Res.string.job_photos),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            stringResource(Res.string.tap_before_after),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        JobPhotoCaptureButtons(
            onCapture = { phase ->
                platformActions.capturePhoto { uri ->
                    uri?.let { onPhotoCaptured(it, phase) }
                }
            }
        )
        CapturedJobPhotoStrip(
            photos = uiState.capturedPhotos,
            onRemovePhoto = onRemovePhoto
        )

        Spacer(Modifier.height(16.dp))
        InvoiceLineItemsList(
            uiState = uiState,
            businessSettings = businessSettings,
            categories = categories,
            onSetReceiptPickerVisible = onSetReceiptPickerVisible,
            onSetInvoiceCategoryDropdownVisible = onSetInvoiceCategoryDropdownVisible,
            onCategoryChange = onCategoryChange,
            onItemDescChange = onItemDescChange,
            onItemAmtChange = onItemAmtChange,
            onProcessInvoiceAi = onProcessInvoiceAi,
            onAddManualLineItem = onAddManualLineItem,
            onRemoveLineItem = onRemoveLineItem
        )

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = uiState.taxText, onValueChange = { onTaxTextChange(it) }, label = { Text(stringResource(Res.string.tax_rate), fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp))
        OutlinedTextField(value = uiState.depositCollected, onValueChange = { onDepositCollectedChange(it) }, label = { Text(stringResource(Res.string.deposit_collected), fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(4.dp))
        
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TacticalButton(onClick = { onSaveInvoice(true, businessSettings) { onShareFile(it, "Estimate") } }, text = stringResource(Res.string.save_estimate), modifier = Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.secondary, enabled = uiState.canSave)
            TacticalButton(onClick = { onSaveInvoice(false, businessSettings) { onShareFile(it, "Invoice") } }, text = stringResource(Res.string.save_invoice), modifier = Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.primary, enabled = uiState.canSave)
        }
        Spacer(Modifier.height(120.dp))
    }

    var selectedReceipt by remember { mutableStateOf<ReceiptItem?>(null) }
    var showMarkupPrompt by remember { mutableStateOf(false) }
    var markupInput by remember { mutableStateOf("20") }

    if (uiState.showReceiptPicker) {
        AlertDialog(
            onDismissRequest = { onSetReceiptPickerVisible(false) },
            title = { Text(stringResource(Res.string.floating_expense_pool), fontWeight = FontWeight.Black) },
            text = {
                if (uiState.availableReceipts.isEmpty()) {
                    Text(stringResource(Res.string.no_unbilled_receipts))
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(uiState.availableReceipts.size) { index ->
                            val receipt = uiState.availableReceipts[index]
                            ListItem(
                                headlineContent = { Text(receipt.description.uppercase(), fontWeight = FontWeight.Black) },
                                supportingContent = { Text(receipt.formattedPrice) },
                                trailingContent = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable { 
                                    selectedReceipt = receipt
                                    showMarkupPrompt = true
                                    onSetReceiptPickerVisible(false)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onSetReceiptPickerVisible(false) }) { Text(stringResource(Res.string.close)) } }
        )
    }

    if (showMarkupPrompt && selectedReceipt != null) {
        AlertDialog(
            onDismissRequest = { showMarkupPrompt = false },
            title = { Text(stringResource(Res.string.apply_markup), fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text(stringResource(Res.string.linking, selectedReceipt!!.description))
                    Text(stringResource(Res.string.raw_cost, selectedReceipt!!.formattedPrice))
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = markupInput,
                        onValueChange = { markupInput = it },
                        label = { Text(stringResource(Res.string.markup_percentage), fontWeight = FontWeight.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        onLinkReceipt(selectedReceipt!!, 0.0)
                        showMarkupPrompt = false
                        selectedReceipt = null
                    }) { Text(stringResource(Res.string.no_at_cost)) }
                    TacticalButton(onClick = {
                        val pct = markupInput.toDoubleOrNull() ?: 0.0
                        onLinkReceipt(selectedReceipt!!, pct)
                        showMarkupPrompt = false
                        selectedReceipt = null
                    }, text = stringResource(Res.string.apply_pct))
                }
            }
        )
    }
}
