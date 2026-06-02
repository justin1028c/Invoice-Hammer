package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.stripe.StripeCardCollectOutcome

interface StripePaymentSheetGateway {
    suspend fun presentPaymentSheet(
        invoice: Invoice,
        type: PaymentRequestType,
        clientSecret: String,
        paymentIntentId: String,
        stripeAccountId: String
    ): StripeCardCollectOutcome
}
