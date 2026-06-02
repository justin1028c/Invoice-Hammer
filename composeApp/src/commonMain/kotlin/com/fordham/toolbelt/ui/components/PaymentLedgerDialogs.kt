package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.fordham.toolbelt.domain.payment.qr.PaymentCheckoutUrl
import com.fordham.toolbelt.ui.theme.BrandOrange

@Composable
fun PaymentRequestCreatedDialog(
    request: InvoicePaymentRequest,
    isLivePowerPay: Boolean,
    isLiveStripe: Boolean,
    onDismiss: () -> Unit,
    onOpenPaymentLink: (String) -> Unit
) {
    val title = when (request.provider) {
        PaymentProviderType.GooglePay -> "GOOGLE PAY LINK READY"
        PaymentProviderType.ApplePay -> "APPLE PAY LINK READY"
        PaymentProviderType.CardLink -> "CARD CHECKOUT READY"
        PaymentProviderType.StellarUsdc -> "STELLAR PAYMENT READY"
        else -> "PAYMENT LINK READY"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${request.invoiceClientName.uppercase()} · ${request.providerLabel} · ${request.formattedAmount}",
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    request.provider.checkoutInstructions(
                        isStellarLive = isLivePowerPay && request.provider.usesStellarRail,
                        isStripeLive = isLiveStripe
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                if (request.paymentLink.value.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ContractorQrPaymentDisplay(
                        checkoutUrl = PaymentCheckoutUrl(request.paymentLink.value),
                        modifier = Modifier.size(200.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    request.paymentLink.value,
                    color = BrandOrange,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
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

@Composable
fun PaymentMethodPickerSheet(
    requestType: PaymentRequestType,
    onDismiss: () -> Unit,
    onProviderSelected: (PaymentProviderType) -> Unit
) {
    val providers = remember {
        listOf(
            PaymentPickerRow(
                PaymentProviderType.GooglePay,
                "Google Pay",
                "Future processor-backed wallet payment.",
                PaymentPickerIcon.Wallet
            ),
            PaymentPickerRow(
                PaymentProviderType.ApplePay,
                "Apple Pay",
                "iOS-ready provider slot for native checkout.",
                PaymentPickerIcon.Phone
            ),
            PaymentPickerRow(
                PaymentProviderType.StellarUsdc,
                "Stellar USDC",
                "Stablecoin settlement rail for SCF demo path.",
                PaymentPickerIcon.Globe
            ),
            PaymentPickerRow(
                PaymentProviderType.CardLink,
                "Card / Payment Link",
                "Hosted fallback link for clients without wallets.",
                PaymentPickerIcon.Card
            ),
            PaymentPickerRow(
                PaymentProviderType.CardTerminal,
                "Card Terminal",
                "Type card on-site (free). Stripe backend enables PCI-safe checkout.",
                PaymentPickerIcon.Card
            ),
            PaymentPickerRow(
                PaymentProviderType.TapToPay,
                "Tap to Pay",
                "Phone NFC checkout — free tier (platform fee via Stripe Connect).",
                PaymentPickerIcon.Phone
            ),
            PaymentPickerRow(
                PaymentProviderType.BluetoothReader,
                "Bluetooth Reader (Pro)",
                "External card reader — Pro subscription required.",
                PaymentPickerIcon.Card
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                item {
                    Text(
                        text = if (requestType == PaymentRequestType.Deposit) "REQUEST DEPOSIT" else "REQUEST FULL PAYMENT",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Stellar USDC → PowerPay. Google Pay, Apple Pay, and Card Link → Stripe. On-site options below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )
                }
                items(providers, key = { it.provider.name }) { row ->
                    PaymentProviderOption(
                        title = row.title,
                        subtitle = row.subtitle,
                        icon = {
                            when (row.icon) {
                                PaymentPickerIcon.Wallet ->
                                    Icon(Icons.Default.Payment, null, tint = BrandOrange)
                                PaymentPickerIcon.Phone ->
                                    Icon(Icons.Default.Smartphone, null, tint = BrandOrange)
                                PaymentPickerIcon.Globe ->
                                    Icon(Icons.Default.Public, null, tint = BrandOrange)
                                PaymentPickerIcon.Card ->
                                    Icon(Icons.Default.CreditCard, null, tint = BrandOrange)
                            }
                        },
                        onClick = { onProviderSelected(row.provider) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

internal data class PaymentPickerRow(
    val provider: PaymentProviderType,
    val title: String,
    val subtitle: String,
    val icon: PaymentPickerIcon
)

internal enum class PaymentPickerIcon {
    Wallet, Phone, Globe, Card
}

@Composable
internal fun PaymentProviderOption(
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
internal fun LedgerMetric(label: String, value: String, modifier: Modifier = Modifier) {
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
internal fun PaymentLedgerRow(
    request: InvoicePaymentRequest,
    onOpenPaymentLink: (String) -> Unit,
    onOpenExplorer: (String) -> Unit = onOpenPaymentLink
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
                if (request.paymentLink.value.isNotBlank()) {
                    TextButton(onClick = { onOpenPaymentLink(request.paymentLink.value) }) {
                        Text("PAY", fontWeight = FontWeight.Black)
                    }
                }
                request.onChainProofUrl?.let { explorer ->
                    TextButton(onClick = { onOpenExplorer(explorer) }) {
                        Text("TX", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

internal fun PaymentRequestType.label(): String = when (this) {
    PaymentRequestType.Deposit -> "DEPOSIT"
    PaymentRequestType.FullBalance -> "FULL PAY"
}
