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
import com.fordham.toolbelt.util.PlatformActions

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
    onCallClient: (String) -> Unit,
    onEmailClient: (String) -> Unit,
    onPhotoCaptured: (String, String) -> Unit,
    platformActions: PlatformActions,
    isPremium: Boolean = false
) {
    // --- DIALOGS ---
    if (uiState.clientToDelete != null) {
        AlertDialog(
            onDismissRequest = { onSetClientToDelete(null) },
            title = { Text("Delete Client?", fontWeight = FontWeight.Black) },
            text = { Text("Are you sure you want to delete ${uiState.clientToDelete!!.name}? This will remove them from your directory, but their invoices and receipts will remain.") },
            confirmButton = { 
                TacticalButton(
                    onClick = { 
                        onDeleteClient(uiState.clientToDelete!!)
                        onSetClientToDelete(null)
                    }, 
                    text = "DELETE", 
                    containerColor = MaterialTheme.colorScheme.error 
                ) 
            },
            dismissButton = { TextButton(onClick = { onSetClientToDelete(null) }) { Text("CANCEL") } }
        )
    }

    if (uiState.showAddNote && selectedClient != null) {
        AlertDialog(
            onDismissRequest = { onSetAddNoteVisible(false) },
            title = { Text("NEW JOB NOTE", fontWeight = FontWeight.Black) }, 
            text = { 
                OutlinedTextField(
                    value = uiState.noteText, 
                    onValueChange = { onSetNoteText(it) }, 
                    label = { Text("NOTE CONTENT...", fontWeight = FontWeight.Bold) }, 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(4.dp)
                ) 
            },
            confirmButton = { 
                TacticalButton(
                    onClick = { onAddNote(selectedClient.name) },
                    text = "SAVE NOTE", 
                    enabled = uiState.noteText.isNotBlank()
                ) 
            }
        )
    }

    if (uiState.showReceiptPicker && selectedClient != null) {
        AlertDialog(
            onDismissRequest = { onSetReceiptPickerVisible(false) },
            title = { Text("FLOATING EXPENSE POOL", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("SELECT A RECEIPT TO LINK TO ${selectedClient.name.uppercase()}:", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
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
            confirmButton = { TextButton(onClick = { onSetReceiptPickerVisible(false) }) { Text("CANCEL") } }
        )
    }

    // --- MAIN CONTENT ---
    if (selectedClient == null) {
        LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            item { Text("CLIENT DIRECTORY", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black) }
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
            
            Text(selectedClient.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
            
            summary?.let {
                ClientFinancialSummaryCard(
                    summary = it,
                    hasAvailableReceipts = uiState.availableReceipts.isNotEmpty(),
                    onLinkReceiptClick = { onSetReceiptPickerVisible(true) }
                )
            }

            Spacer(Modifier.height(24.dp))
            
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
                    onSnapPhotoClick = { 
                        platformActions.capturePhoto { uri ->
                            uri?.let { onPhotoCaptured(it, clientInvoices.first().id.value) }
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
            ClientInvoicesSection(
                invoices = clientInvoices,
                onInvoiceClick = { if (it.pdfPath.isNotEmpty()) onViewPdf(it.pdfPath) },
                onAddPhotoClick = { inv ->
                    platformActions.capturePhoto { uri ->
                        uri?.let { onPhotoCaptured(it, inv.id.value) }
                    }
                }
            )
            
            Spacer(Modifier.height(100.dp))
        }
    }
}

private fun Modifier.fillContentSize() = this.fillMaxWidth()
