package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.checkoutInstructions
import com.fordham.toolbelt.domain.model.usesStellarRail
import com.fordham.toolbelt.ui.theme.BrandOrange
import com.fordham.toolbelt.ui.viewmodel.PaymentUiState
import com.fordham.toolbelt.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentLedgerSheet(
    uiState: PaymentUiState,
    onDismiss: () -> Unit,
    onOpenPaymentLink: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = BrandOrange)
                Column {
                    Text("PAYMENT LEDGER", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        uiState.connectionBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                LedgerMetric("REQUESTED", DateTimeUtil.formatMoney(uiState.totalRequested), Modifier.weight(1f))
                LedgerMetric("ACTIVE", uiState.pendingCount.toString(), Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            uiState.lastReceiptUrl?.let { receiptUrl ->
                TextButton(onClick = { onOpenPaymentLink(receiptUrl) }) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    Text(
                        if (uiState.lastPaidInvoiceId != null) {
                            "Invoice ${uiState.lastPaidInvoiceId} paid — view receipt"
                        } else {
                            "View payment receipt"
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (uiState.requests.isEmpty()) {
                Text(
                    "No payment requests yet. Open an invoice and request a deposit or full payment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    items(uiState.requests, key = { it.id.value }) { request ->
                        PaymentLedgerRow(request = request, onOpenPaymentLink = onOpenPaymentLink)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
