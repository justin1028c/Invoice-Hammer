package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.payment.qr.PaymentCheckoutUrl
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

@Composable
fun ContractorQrPaymentDisplay(
    checkoutUrl: PaymentCheckoutUrl,
    modifier: Modifier = Modifier,
    onOpenCheckout: (() -> Unit)? = null
) {
    val encodedUrl = PaymentCheckoutUrl.forQrEncoding(checkoutUrl.value)
    val qrPainter = rememberQrCodePainter(encodedUrl.value)
    val scanHint = stringResource(Res.string.qr_scan_hint)
    val tapHint = stringResource(Res.string.qr_tap_to_open)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .background(Color.White)
                .then(
                    if (onOpenCheckout != null) {
                        Modifier.clickable(onClick = onOpenCheckout)
                    } else {
                        Modifier
                    }
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = qrPainter,
                contentDescription = scanHint,
                modifier = Modifier
                    .matchParentSize()
                    .padding(4.dp)
            )
        }

        if (onOpenCheckout != null) {
            Text(
                text = tapHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
