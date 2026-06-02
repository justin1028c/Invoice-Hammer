package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.payment.qr.PaymentCheckoutUrl
import com.fordham.toolbelt.presentation.payment.qr.QrCodeMatrixEngine

@Composable
fun ContractorQrPaymentDisplay(
    checkoutUrl: PaymentCheckoutUrl,
    modifier: Modifier = Modifier
) {
    // Generate matrix and memoize the path construction to prevent recomposition overhead
    val qrMatrix = remember(checkoutUrl) { QrCodeMatrixEngine.encodeUrl(checkoutUrl) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.White)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (qrMatrix.size == 0) return@Canvas

            val canvasWidth = size.width
            val moduleSize = canvasWidth / qrMatrix.size

            // Construct a single Path instead of thousands of individual Rects
            val qrPath = Path().apply {
                for (row in 0 until qrMatrix.size) {
                    for (col in 0 until qrMatrix.size) {
                        if (qrMatrix.isDark(row, col)) {
                            addRect(
                                Rect(
                                    left = col * moduleSize,
                                    top = row * moduleSize,
                                    right = (col + 1) * moduleSize,
                                    bottom = (row + 1) * moduleSize
                                )
                            )
                        }
                    }
                }
            }

            drawPath(
                path = qrPath,
                brush = SolidColor(Color(0xFF1E1E1E))
            )
        }
    }
}
