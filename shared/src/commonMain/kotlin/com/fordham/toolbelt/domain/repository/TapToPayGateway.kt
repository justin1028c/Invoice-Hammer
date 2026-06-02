package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.stripe.TapToPayOutcome

/** Phone NFC Tap to Pay — free tier (platform fee via Connect). */
interface TapToPayGateway {
    suspend fun collect(
        invoice: Invoice,
        type: PaymentRequestType,
        clientSecret: String,
        paymentIntentId: String,
        stripeAccountId: String
    ): TapToPayOutcome
}
