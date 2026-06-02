package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.stripe.StripeCheckoutSessionOutcome

interface StripeCheckoutRepository {
    suspend fun generateFallbackPaymentLink(
        invoiceId: InvoiceId,
        amountInCents: Long,
        contractorUserId: String
    ): StripeCheckoutSessionOutcome
}
