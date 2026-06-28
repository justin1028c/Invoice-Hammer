package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.ui.theme.BrandOrange
import com.fordham.toolbelt.ui.viewmodel.PaymentUiState
import com.fordham.toolbelt.util.DateTimeUtil
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

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
                    Text(stringResource(Res.string.payment_ledger_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        "Track and manage invoice client payments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                LedgerMetric(stringResource(Res.string.requested_metric), DateTimeUtil.formatMoney(uiState.totalRequested), Modifier.weight(1f))
                LedgerMetric(stringResource(Res.string.active_metric), uiState.pendingCount.toString(), Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            uiState.lastReceiptUrl?.let { receiptUrl ->
                TextButton(onClick = { onOpenPaymentLink(receiptUrl) }) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    Text(
                        if (uiState.lastPaidInvoiceId != null) {
                            stringResource(Res.string.invoice_paid_view_receipt, uiState.lastPaidInvoiceId)
                        } else {
                            stringResource(Res.string.view_payment_receipt)
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (uiState.requests.isEmpty()) {
                Text(
                    stringResource(Res.string.no_payment_requests_yet),
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
