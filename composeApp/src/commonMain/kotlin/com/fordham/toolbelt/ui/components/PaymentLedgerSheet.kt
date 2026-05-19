package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestType
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
                    Text("Google Pay, Apple Pay, Stellar, and card-link requests.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                LedgerMetric("REQUESTED", DateTimeUtil.formatMoney(uiState.totalRequested), Modifier.weight(1f))
                LedgerMetric("ACTIVE", uiState.pendingCount.toString(), Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

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

@Composable
fun PaymentRequestCreatedDialog(
    request: InvoicePaymentRequest,
    onDismiss: () -> Unit,
    onOpenPaymentLink: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PAYMENT LINK READY", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${request.invoiceClientName.uppercase()} - ${request.formattedAmount}", fontWeight = FontWeight.Black)
                Text("Mock ${request.providerLabel} payment request created. This is the app-side flow your backend can replace later.")
                Text(request.paymentLink.value, color = BrandOrange, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            TacticalButton(
                onClick = { onOpenPaymentLink(request.paymentLink.value) },
                text = "OPEN LINK",
                icon = { Icon(Icons.Default.OpenInBrowser, null) }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("DONE") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodPickerSheet(
    requestType: PaymentRequestType,
    onDismiss: () -> Unit,
    onProviderSelected: (PaymentProviderType) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = if (requestType == PaymentRequestType.Deposit) "REQUEST DEPOSIT" else "REQUEST FULL PAYMENT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                "Choose the rail for this invoice payment. These are mock flows until the backend is connected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            PaymentProviderOption(
                title = "Google Pay",
                subtitle = "Future processor-backed wallet payment.",
                icon = { Icon(Icons.Default.Payment, null, tint = BrandOrange) },
                onClick = { onProviderSelected(PaymentProviderType.GooglePay) }
            )
            PaymentProviderOption(
                title = "Apple Pay",
                subtitle = "iOS-ready provider slot for native checkout.",
                icon = { Icon(Icons.Default.Smartphone, null, tint = BrandOrange) },
                onClick = { onProviderSelected(PaymentProviderType.ApplePay) }
            )
            PaymentProviderOption(
                title = "Stellar USDC",
                subtitle = "Stablecoin settlement rail for SCF demo path.",
                icon = { Icon(Icons.Default.Public, null, tint = BrandOrange) },
                onClick = { onProviderSelected(PaymentProviderType.StellarUsdc) }
            )
            PaymentProviderOption(
                title = "Card / Payment Link",
                subtitle = "Hosted fallback link for clients without wallets.",
                icon = { Icon(Icons.Default.CreditCard, null, tint = BrandOrange) },
                onClick = { onProviderSelected(PaymentProviderType.CardLink) }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PaymentProviderOption(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(title.uppercase(), fontWeight = FontWeight.Black)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LedgerMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
            Text(value, style = MaterialTheme.typography.titleMedium, color = BrandOrange, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PaymentLedgerRow(
    request: InvoicePaymentRequest,
    onOpenPaymentLink: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(request.invoiceClientName.uppercase(), fontWeight = FontWeight.Black)
                Text(
                    "${request.type.label()} • ${request.providerLabel} • ${request.statusLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(request.formattedAmount, color = BrandOrange, fontWeight = FontWeight.Black)
                TextButton(onClick = { onOpenPaymentLink(request.paymentLink.value) }) {
                    Text("LINK", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

private fun PaymentRequestType.label(): String = when (this) {
    PaymentRequestType.Deposit -> "DEPOSIT"
    PaymentRequestType.FullBalance -> "FULL PAY"
}
