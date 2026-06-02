package com.fordham.toolbelt.domain.payment.qr

import kotlin.jvm.JvmInline

@JvmInline
value class PaymentCheckoutUrl(val value: String) {
    init {
        require(value.isNotBlank()) { "Checkout URL cannot be blank" }
    }
}

sealed interface QrSessionState {
    data object Idle : QrSessionState
    data object Generating : QrSessionState
    data class Success(val checkoutUrl: PaymentCheckoutUrl) : QrSessionState
    data class Failure(val message: String) : QrSessionState
}
