package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.fordham.toolbelt.domain.model.InvoicePaymentStatus
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.payment.qr.PaymentCheckoutUrl
import com.fordham.toolbelt.ui.localizeUiMessage
import com.fordham.toolbelt.ui.theme.BrandOrange
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

@Composable
fun PaymentRequestCreatedDialog(
    request: InvoicePaymentRequest,
    isLiveStripe: Boolean,
    checkoutUrl: String?,
    checkoutLinkCanPay: Boolean,
    checkoutLinkMessage: String?,
    isResolvingCheckoutLink: Boolean,
    onDismiss: () -> Unit,
    onOpenPaymentLink: (String) -> Unit,
    onRegenerateLink: () -> Unit
) {
    val clientScanHint = stringResource(Res.string.checkout_link_client_hint)
    val refreshingText = stringResource(Res.string.checkout_link_refreshing)
    val newLinkText = stringResource(Res.string.checkout_link_new)
    val localizedCheckoutMessage = checkoutLinkMessage?.let { localizeUiMessage(it) }
    val displayUrl = checkoutUrl?.takeIf { it.isNotBlank() && checkoutLinkCanPay }
    val title = when (request.provider) {
        PaymentProviderType.GooglePay -> stringResource(Res.string.google_pay_ready)
        PaymentProviderType.ApplePay -> stringResource(Res.string.apple_pay_ready)
        PaymentProviderType.CardLink -> stringResource(Res.string.card_checkout_ready)
        else -> stringResource(Res.string.payment_link_ready)
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
                    "${request.invoiceClientName.uppercase()} · ${request.provider.localizedLabel()} · ${request.formattedAmount}",
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    request.provider.checkoutInstructions(
                        isStripeLive = isLiveStripe
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                if (isLiveStripe && checkoutLinkCanPay) {
                    Text(
                        clientScanHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
                if (isResolvingCheckoutLink) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
                    Text(refreshingText, style = MaterialTheme.typography.bodySmall)
                }
                localizedCheckoutMessage?.let { message ->
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
                displayUrl?.let { url ->
                    Spacer(modifier = Modifier.height(8.dp))
                    ContractorQrPaymentDisplay(
                        checkoutUrl = PaymentCheckoutUrl.forQrEncoding(url),
                        modifier = Modifier.size(200.dp),
                        onOpenCheckout = { onOpenPaymentLink(url) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        url,
                        color = BrandOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
            }
        },
        confirmButton = {
            when {
                checkoutLinkCanPay && !displayUrl.isNullOrBlank() -> {
                    TacticalButton(
                        onClick = { onOpenPaymentLink(displayUrl) },
                        text = stringResource(Res.string.open_link),
                        icon = { Icon(Icons.Default.OpenInBrowser, null) }
                    )
                }
                !checkoutLinkCanPay -> {
                    TacticalButton(
                        onClick = onRegenerateLink,
                        text = newLinkText,
                        icon = { Icon(Icons.Default.Refresh, null) }
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.done)) } }
    )
}

@Composable
fun PaymentMethodPickerSheet(
    requestType: PaymentRequestType,
    onDismiss: () -> Unit,
    onProviderSelected: (PaymentProviderType) -> Unit
) {
    val googlePayDesc = stringResource(Res.string.google_pay_picker_desc)
    val applePayDesc = stringResource(Res.string.apple_pay_picker_desc)
    val cardLinkTitle = stringResource(Res.string.card_link_picker_title)
    val cardLinkDesc = stringResource(Res.string.card_link_picker_desc)
    val cardTerminalTitle = stringResource(Res.string.card_terminal_picker_title)
    val cardTerminalDesc = stringResource(Res.string.card_terminal_picker_desc)
    val tapToPayTitle = stringResource(Res.string.tap_to_pay_picker_title)
    val tapToPayDesc = stringResource(Res.string.tap_to_pay_picker_desc)
    val bluetoothReaderTitle = stringResource(Res.string.bluetooth_reader_picker_title)
    val bluetoothReaderDesc = stringResource(Res.string.bluetooth_reader_picker_desc)

    val providers = remember(
        googlePayDesc, applePayDesc, cardLinkTitle, cardLinkDesc,
        cardTerminalTitle, cardTerminalDesc, tapToPayTitle, tapToPayDesc,
        bluetoothReaderTitle, bluetoothReaderDesc
    ) {
        listOf(
            PaymentPickerRow(
                PaymentProviderType.GooglePay,
                "Google Pay",
                googlePayDesc,
                PaymentPickerIcon.Wallet
            ),
            PaymentPickerRow(
                PaymentProviderType.ApplePay,
                "Apple Pay",
                applePayDesc,
                PaymentPickerIcon.Phone
            ),
            PaymentPickerRow(
                PaymentProviderType.CardLink,
                cardLinkTitle,
                cardLinkDesc,
                PaymentPickerIcon.Card
            ),
            PaymentPickerRow(
                PaymentProviderType.CardTerminal,
                cardTerminalTitle,
                cardTerminalDesc,
                PaymentPickerIcon.Card
            ),
            PaymentPickerRow(
                PaymentProviderType.TapToPay,
                tapToPayTitle,
                tapToPayDesc,
                PaymentPickerIcon.Phone
            ),
            PaymentPickerRow(
                PaymentProviderType.BluetoothReader,
                bluetoothReaderTitle,
                bluetoothReaderDesc,
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
                        text = if (requestType == PaymentRequestType.Deposit) stringResource(Res.string.deposit_type_label) else stringResource(Res.string.request_full_payment),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        stringResource(Res.string.payment_picker_subtitle),
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
    onSelectRequest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(onClick = onSelectRequest),
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
                    "${request.type.label()} • ${request.provider.localizedLabel()} • ${request.status.localizedLabel()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(request.formattedAmount, color = BrandOrange, fontWeight = FontWeight.Black)
                if (request.paymentLink.value.isNotBlank()) {
                    TextButton(onClick = { onOpenPaymentLink(request.paymentLink.value) }) {
                        Text(stringResource(Res.string.pay_label), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PaymentRequestType.label(): String = when (this) {
    PaymentRequestType.Deposit -> stringResource(Res.string.deposit_type_label)
    PaymentRequestType.FullBalance -> stringResource(Res.string.full_pay_type_label)
}

@Composable
fun PaymentProviderType.localizedLabel(): String = when (this) {
    PaymentProviderType.GooglePay -> "Google Pay"
    PaymentProviderType.ApplePay -> "Apple Pay"
    PaymentProviderType.CardLink -> stringResource(Res.string.card_link_picker_title)
    PaymentProviderType.CardTerminal -> stringResource(Res.string.card_terminal_picker_title)
    PaymentProviderType.TapToPay -> stringResource(Res.string.tap_to_pay_picker_title)
    PaymentProviderType.BluetoothReader -> stringResource(Res.string.bluetooth_reader_picker_title)
}

@Composable
fun InvoicePaymentStatus.localizedLabel(): String = when (this) {
    InvoicePaymentStatus.Requested -> stringResource(Res.string.status_requested)
    InvoicePaymentStatus.Pending -> stringResource(Res.string.status_pending)
    InvoicePaymentStatus.Paid -> stringResource(Res.string.status_paid)
    InvoicePaymentStatus.Failed -> stringResource(Res.string.status_failed)
    InvoicePaymentStatus.Expired -> stringResource(Res.string.status_expired)
}

@Composable
fun PaymentProviderType.checkoutInstructions(
    isStripeLive: Boolean
): String = when (this) {
    PaymentProviderType.GooglePay ->
        if (isStripeLive) {
            stringResource(Res.string.checkout_instr_gpay_live)
        } else {
            stringResource(Res.string.checkout_instr_gpay_demo)
        }
    PaymentProviderType.ApplePay ->
        if (isStripeLive) {
            stringResource(Res.string.checkout_instr_apay_live)
        } else {
            stringResource(Res.string.checkout_instr_apay_demo)
        }
    PaymentProviderType.CardLink ->
        if (isStripeLive) {
            stringResource(Res.string.checkout_instr_card_live)
        } else {
            stringResource(Res.string.checkout_instr_card_demo)
        }
    else -> stringResource(Res.string.checkout_instr_fallback)
}
