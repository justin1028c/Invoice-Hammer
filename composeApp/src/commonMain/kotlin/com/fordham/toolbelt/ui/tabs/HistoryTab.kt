package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.ui.components.HistoryItemCard
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.viewmodel.HistoryUiState
import com.fordham.toolbelt.util.PlatformActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTab(
    uiState: HistoryUiState,
    filteredHistory: List<Invoice>,
    paymentRequests: List<InvoicePaymentRequest>,
    onViewPdf: (String) -> Unit,
    onSharePdf: (String, String) -> Unit,
    onRequestDeposit: (Invoice) -> Unit,
    onRequestFullPayment: (Invoice) -> Unit,
    onSetInvoiceToDelete: (Invoice?) -> Unit,
    onDeleteInvoice: (Invoice) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onShowPaidOnlyChange: (Boolean) -> Unit,
    onUpdateInvoice: (Invoice) -> Unit,
    onConvertEstimateToInvoice: (Invoice) -> Unit,
    platformActions: PlatformActions,
    listScrollEnabled: Boolean = true
) {
    if (uiState.invoiceToDelete != null) {
        AlertDialog(
            onDismissRequest = { onSetInvoiceToDelete(null) },
            title = { Text("Delete Record?", fontWeight = FontWeight.Black) },
            text = { Text("This will permanently remove this ${if (uiState.invoiceToDelete!!.isEstimate) "estimate" else "invoice"} from your records. The PDF file will remain on your device.") },
            confirmButton = { 
                TacticalButton(
                    onClick = { 
                        onDeleteInvoice(uiState.invoiceToDelete!!)
                        onSetInvoiceToDelete(null)
                    }, 
                    text = "DELETE", 
                    containerColor = MaterialTheme.colorScheme.error
                ) 
            },
            dismissButton = { 
                TextButton(onClick = { onSetInvoiceToDelete(null) }) {
                    Text("CANCEL") 
                } 
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        userScrollEnabled = listScrollEnabled
    ) {
        item { 
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "ARCHIVED RECORDS", 
                    style = MaterialTheme.typography.headlineSmall, 
                    color = MaterialTheme.colorScheme.onBackground, 
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    "Track your cash flow history.", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("SEARCH CLIENTS OR ITEMS...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Close, null) } }
                    } else null,
                    shape = RoundedCornerShape(4.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.showPaidOnly,
                        onClick = { onShowPaidOnlyChange(!uiState.showPaidOnly) },
                        label = { Text("PAID ONLY", fontWeight = FontWeight.Black) },
                        shape = RoundedCornerShape(4.dp),
                        leadingIcon = if (uiState.showPaidOnly) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.showPaidOnly,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }

    items(filteredHistory, key = { it.id.value }) { invoice -> 
            HistoryItemCard(
                invoice = invoice,
                paymentRequest = paymentRequests.firstOrNull { it.invoiceId == invoice.id },
                onDelete = { onSetInvoiceToDelete(it) },
                onTogglePaid = { onUpdateInvoice(it.copy(isPaid = !it.isPaid)) },
                onView = { if (it.pdfPath.isNotEmpty()) onViewPdf(it.pdfPath) },
                onShare = { onSharePdf(it.pdfPath, if (it.isEstimate) "Estimate" else "Invoice") },
                onRequestDeposit = onRequestDeposit,
                onRequestFullPayment = onRequestFullPayment,
                onConvert = if (invoice.isEstimate) { 
                    { est -> 
                        onConvertEstimateToInvoice(est)
                        platformActions.showToast("Converted!")
                    } 
                } else null
            ) 
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}
