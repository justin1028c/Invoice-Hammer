package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.cardterminal.CardBrand
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalDraft
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPhase
import com.fordham.toolbelt.domain.model.cardterminal.phaseLabel
import com.fordham.toolbelt.domain.model.stripe.StripePaymentMode
import com.fordham.toolbelt.domain.util.CardTerminalValidator
import com.fordham.toolbelt.ui.theme.BrandOrange
import com.fordham.toolbelt.ui.viewmodel.CardTerminalUiState
import com.fordham.toolbelt.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardTerminalSheet(
    invoice: Invoice,
    requestType: PaymentRequestType,
    uiState: CardTerminalUiState,
    stripePaymentMode: StripePaymentMode,
    onDismiss: () -> Unit,
    onDraftChange: (CardTerminalDraft) -> Unit,
    onSecureCheckout: () -> Unit,
    onManualCharge: () -> Unit
) {
    val amount = remember(invoice, requestType) {
        when (requestType) {
            PaymentRequestType.Deposit ->
                invoice.depositAmount.takeIf { it > 0.0 } ?: invoice.totalAmount * 0.30
            PaymentRequestType.FullBalance -> invoice.totalAmount
        }
    }

    val panDigits = uiState.draft.panDigits.filter { it.isDigit() }
    val brand = remember(panDigits) { CardTerminalValidator.detectBrand(panDigits) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.CreditCard, null, tint = BrandOrange)
                Column {
                    Text("CARD TERMINAL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        "On-site entry · ${invoice.clientName.uppercase()} · ${DateTimeUtil.formatMoney(amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (stripePaymentMode == StripePaymentMode.PaymentSheet) {
                Text(
                    "Stripe secure checkout — card data never touches Invoice Hammer servers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                TacticalButton(
                    onClick = onSecureCheckout,
                    text = "SECURE CHECKOUT ${DateTimeUtil.formatMoney(amount)}",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isProcessing
                )
                Spacer(Modifier.height(16.dp))
            }

            if (stripePaymentMode == StripePaymentMode.ManualEntrySimulator) {
                VirtualCardPreview(
                brand = brand,
                panDisplay = CardTerminalValidator.formatPanDisplay(panDigits).ifBlank { "•••• •••• •••• ••••" },
                cardholder = uiState.draft.cardholderName.ifBlank { "CARDHOLDER NAME" },
                expiry = uiState.draft.expiryInput.ifBlank { "MM/YY" }
            )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                value = CardTerminalValidator.formatPanDisplay(panDigits),
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(19)
                    onDraftChange(uiState.draft.copy(panDigits = digits))
                },
                label = { Text("Card number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isProcessing
            )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.draft.expiryInput,
                    onValueChange = { raw ->
                        onDraftChange(uiState.draft.copy(expiryInput = CardTerminalValidator.formatExpiryInput(raw)))
                    },
                    label = { Text("Expiry") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !uiState.isProcessing
                )
                OutlinedTextField(
                    value = uiState.draft.cvvDigits,
                    onValueChange = { raw ->
                        val max = if (brand == CardBrand.Amex) 4 else 3
                        onDraftChange(uiState.draft.copy(cvvDigits = raw.filter { it.isDigit() }.take(max)))
                    },
                    label = { Text("CVV") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !uiState.isProcessing
                )
            }

                OutlinedTextField(
                value = uiState.draft.cardholderName,
                onValueChange = { onDraftChange(uiState.draft.copy(cardholderName = it.uppercase())) },
                label = { Text("Cardholder name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isProcessing
            )

                TacticalButton(
                    onClick = onManualCharge,
                    text = if (uiState.isProcessing) "PROCESSING…" else "CHARGE ${DateTimeUtil.formatMoney(amount)}",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isProcessing
                )

                Text(
                    "Beta simulator — add Stripe keys for live PCI-safe checkout. Use ••••0002 to simulate decline.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(top = 8.dp)
            ) {
                uiState.errorMessage?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                } ?: uiState.successMessage?.let { msg ->
                    Text(msg, color = BrandOrange, fontWeight = FontWeight.Bold)
                }
            }

            if (uiState.phase != CardTerminalPhase.Idle) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    uiState.phase.phaseLabel,
                    modifier = Modifier.padding(top = 6.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VirtualCardPreview(
    brand: CardBrand,
    panDisplay: String,
    cardholder: String,
    expiry: String
) {
    val gradient = when (brand) {
        CardBrand.Visa -> Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF3949AB)))
        CardBrand.Mastercard -> Brush.linearGradient(listOf(Color(0xFF424242), Color(0xFFE65100)))
        CardBrand.Amex -> Brush.linearGradient(listOf(Color(0xFF00695C), Color(0xFF00838F)))
        CardBrand.Discover -> Brush.linearGradient(listOf(Color(0xFF4E342E), Color(0xFFFF8F00)))
        CardBrand.Unknown -> Brush.linearGradient(listOf(Color(0xFF212121), Color(0xFF616161)))
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                brand.name.uppercase().ifBlank { "CARD" },
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(panDisplay, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(cardholder, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(expiry, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
            }
        }
    }
}
