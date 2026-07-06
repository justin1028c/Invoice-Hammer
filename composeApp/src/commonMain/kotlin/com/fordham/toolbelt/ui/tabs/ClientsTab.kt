package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.usecase.FinancialSummary
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.tabs.clients.*
import com.fordham.toolbelt.ui.viewmodel.ClientsUiState
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.PlatformActions
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.TrendingUp

/**
 * Responsibility: Main orchestration for the Client Directory & Profiles.
 * ADHERENCE: Below 300 line limit.
 */
@Composable
fun ClientsTab(
    clients: List<Client>,
    selectedClient: Client?,
    clientInvoices: List<Invoice>,
    summary: FinancialSummary?,
    jobNotes: List<JobNote>,
    jobPhotos: List<JobPhoto>,
    uiState: ClientsUiState,
    onClientClick: (Client) -> Unit,
    onDeleteClient: (Client) -> Unit,
    onSetClientToDelete: (Client?) -> Unit,
    onBackClick: () -> Unit,
    onAddNote: (String) -> Unit,
    onDeleteNote: (JobNote) -> Unit,
    onSummarizeNotes: () -> Unit,
    onLinkReceipt: (ReceiptItem) -> Unit,
    onViewPdf: (String) -> Unit,
    onSetNoteText: (String) -> Unit,
    onSetAddNoteVisible: (Boolean) -> Unit,
    onSetReceiptPickerVisible: (Boolean) -> Unit,
    onClearAiSummary: () -> Unit,
    onNewInvoiceForClient: (Client) -> Unit,
    onEditInvoiceAsDraft: (Invoice) -> Unit,
    onCallClient: (String) -> Unit,
    onEmailClient: (String) -> Unit,
    onPhotoCaptured: (String, String, JobPhotoPhase) -> Unit,
    onSetEditProfileVisible: (Boolean, Client?) -> Unit,
    onEditFieldsChange: (String, String, String, String) -> Unit,
    onSaveProfile: (Client) -> Unit,
    platformActions: PlatformActions,
    isPremium: Boolean = false
) {
    // --- DIALOGS ---
    if (uiState.clientToDelete != null) {
        AlertDialog(
            onDismissRequest = { onSetClientToDelete(null) },
            title = { Text(stringResource(Res.string.delete_client), fontWeight = FontWeight.Black) },
            text = { Text(stringResource(Res.string.delete_client_desc, uiState.clientToDelete!!.name.value)) },
            confirmButton = { 
                TacticalButton(
                    onClick = { 
                        onDeleteClient(uiState.clientToDelete!!)
                        onSetClientToDelete(null)
                    }, 
                    text = stringResource(Res.string.delete), 
                    containerColor = MaterialTheme.colorScheme.error 
                ) 
            },
            dismissButton = { TextButton(onClick = { onSetClientToDelete(null) }) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    if (uiState.showAddNote && selectedClient != null) {
        AlertDialog(
            onDismissRequest = { onSetAddNoteVisible(false) },
            title = { Text(stringResource(Res.string.new_job_note), fontWeight = FontWeight.Black) }, 
            text = { 
                OutlinedTextField(
                    value = uiState.noteText, 
                    onValueChange = { onSetNoteText(it) }, 
                    label = { Text(stringResource(Res.string.note_content), fontWeight = FontWeight.Bold) }, 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(4.dp)
                ) 
            },
            confirmButton = { 
                TacticalButton(
                    onClick = { onAddNote(selectedClient.name.value) },
                    text = stringResource(Res.string.save_note), 
                    enabled = uiState.noteText.isNotBlank()
                ) 
            }
        )
    }

    if (uiState.showReceiptPicker && selectedClient != null) {
        AlertDialog(
            onDismissRequest = { onSetReceiptPickerVisible(false) },
            title = { Text(stringResource(Res.string.floating_expense_pool), fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text(stringResource(Res.string.select_receipt_link, selectedClient.name.value.uppercase()), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(uiState.availableReceipts) { receipt ->
                            ListItem(
                                headlineContent = { Text(receipt.description.uppercase(), fontWeight = FontWeight.Black) },
                                supportingContent = { Text(receipt.formattedPrice) },
                                modifier = Modifier.fillContentSize().clickable { onLinkReceipt(receipt) }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onSetReceiptPickerVisible(false) }) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    if (uiState.showEditProfile && selectedClient != null) {
        AlertDialog(
            onDismissRequest = { onSetEditProfileVisible(false, null) },
            title = { Text(stringResource(Res.string.edit_client_profile), fontWeight = FontWeight.Black) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.editName,
                        onValueChange = { onEditFieldsChange(it, uiState.editAddress, uiState.editPhone, uiState.editEmail) },
                        label = { Text(stringResource(Res.string.client_name), fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.editAddress,
                        onValueChange = { onEditFieldsChange(uiState.editName, it, uiState.editPhone, uiState.editEmail) },
                        label = { Text(stringResource(Res.string.client_address), fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.editPhone,
                        onValueChange = { onEditFieldsChange(uiState.editName, uiState.editAddress, it, uiState.editEmail) },
                        label = { Text(stringResource(Res.string.phone_number), fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.editEmail,
                        onValueChange = { onEditFieldsChange(uiState.editName, uiState.editAddress, uiState.editPhone, it) },
                        label = { Text(stringResource(Res.string.email_address), fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TacticalButton(
                    onClick = { onSaveProfile(selectedClient) },
                    text = stringResource(Res.string.save),
                    enabled = uiState.editName.isNotBlank()
                )
            },
            dismissButton = {
                TextButton(onClick = { onSetEditProfileVisible(false, null) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    // --- MAIN CONTENT ---
    if (selectedClient == null) {
        LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            item { Text(stringResource(Res.string.client_directory), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black) }
            items(clients) { client -> 
                ClientDirectoryItem(
                    client = client,
                    onClick = { onClientClick(client) },
                    onDeleteClick = { onSetClientToDelete(client) }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outline) }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ClientProfileHeader(
                client = selectedClient,
                onBackClick = onBackClick,
                onCallClick = onCallClient,
                onEmailClick = onEmailClient
            )

            ClientProfileDashboard(
                client = selectedClient,
                invoices = clientInvoices,
                summary = summary
            )

            Spacer(Modifier.height(8.dp))

            ClientQuickActions(
                client = selectedClient,
                hasInvoices = clientInvoices.isNotEmpty(),
                hasLastPdf = clientInvoices.any { it.pdfPath.value.isNotEmpty() },
                onNewInvoiceClick = { onNewInvoiceForClient(selectedClient) },
                onDuplicateLastClick = {
                    clientInvoices.firstOrNull()?.let(onEditInvoiceAsDraft)
                },
                onLastInvoiceClick = {
                    clientInvoices.firstOrNull { it.pdfPath.value.isNotEmpty() }?.let { onViewPdf(it.pdfPath.value) }
                },
                onAddNoteClick = { onSetAddNoteVisible(true) },
                onEditProfileClick = { onSetEditProfileVisible(true, selectedClient) },
                onCallClick = onCallClient,
                onEmailClick = onEmailClient
            )

            summary?.let {
                ClientFinancialSummaryCard(
                    summary = it,
                    hasAvailableReceipts = uiState.availableReceipts.isNotEmpty(),
                    onLinkReceiptClick = { onSetReceiptPickerVisible(true) }
                )
            }

            Spacer(Modifier.height(16.dp))
            
            ClientNotesSection(
                jobNotes = jobNotes,
                isPremium = isPremium,
                isSummarizing = uiState.isSummarizing,
                aiSummary = uiState.aiSummary,
                onSummarizeClick = onSummarizeNotes,
                onAddNoteClick = { onSetAddNoteVisible(true) },
                onDeleteNoteClick = onDeleteNote,
                onClearAiSummary = onClearAiSummary
            )

            if (jobPhotos.isNotEmpty() || clientInvoices.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                ClientPhotosSection(
                    jobPhotos = jobPhotos,
                    canCapture = clientInvoices.isNotEmpty(),
                    onCapturePhoto = { phase ->
                        platformActions.capturePhoto { uri ->
                            uri?.let { onPhotoCaptured(it, clientInvoices.first().id.value, phase) }
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
            ClientInvoicesSection(
                invoices = clientInvoices,
                onInvoiceClick = { if (it.pdfPath.value.isNotEmpty()) onViewPdf(it.pdfPath.value) },
                onEditAsDraftClick = onEditInvoiceAsDraft,
                onAddPhotoClick = { inv, phase ->
                    platformActions.capturePhoto { uri ->
                        uri?.let { onPhotoCaptured(it, inv.id.value, phase) }
                    }
                }
            )

            Spacer(Modifier.height(100.dp))
        }
    }
}

private fun Modifier.fillContentSize() = this.fillMaxWidth()

private fun getClientInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+"))
    if (parts.isEmpty()) return "?"
    val first = parts.first().firstOrNull()?.uppercaseChar() ?: '?'
    val last = if (parts.size > 1) parts.last().firstOrNull()?.uppercaseChar() else null
    return if (last != null) "$first$last" else "$first"
}

@Composable
private fun ClientProfileDashboard(
    client: Client,
    invoices: List<Invoice>,
    summary: FinancialSummary?,
    modifier: Modifier = Modifier
) {
    val unpaid = invoices.filterNot { it.isPaid || it.isEstimate }.map { it.totalAmount.value }.sum()
    val lastInvoice = invoices.maxByOrNull { it.lastUpdated }
    val initials = getClientInitials(client.name.value)
    
    Card(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = client.name.value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = client.address.value.ifBlank { stringResource(Res.string.no_address_on_file) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClientMetric(
                    label = stringResource(Res.string.stat_invoices),
                    value = invoices.size.toString(),
                    icon = Icons.Default.Receipt,
                    modifier = Modifier.weight(1f)
                )
                ClientMetric(
                    label = stringResource(Res.string.stat_unpaid),
                    value = DateTimeUtil.formatMoney(unpaid),
                    icon = Icons.Default.PendingActions,
                    valueColor = if (unpaid > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ClientMetric(
                    label = stringResource(Res.string.stat_net_profit),
                    value = summary?.formattedProfit ?: "$0.00",
                    icon = Icons.Default.TrendingUp,
                    valueColor = if ((summary?.profit ?: 0.0) >= 0.0) com.fordham.toolbelt.ui.theme.StatsGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        Res.string.last_job_prefix,
                        lastInvoice?.let { DateTimeUtil.formatDateForDisplay(it.date) } ?: stringResource(Res.string.none_label)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (lastInvoice != null) {
                    Text(
                        text = lastInvoice.formattedTotal,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientMetric(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                color = valueColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 10.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

