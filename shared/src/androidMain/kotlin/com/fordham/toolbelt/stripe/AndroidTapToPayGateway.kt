package com.fordham.toolbelt.stripe

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.stripe.TapToPayOutcome
import com.fordham.toolbelt.domain.repository.TapToPayGateway

/**
 * Phase 1: routes to the same PCI-safe Payment Sheet while Stripe Terminal Tap to Pay
 * registration is completed. NFC collection does not use manual PAN entry.
 */
class AndroidTapToPayGateway(
    private val paymentSheetGateway: AndroidStripePaymentSheetGateway
) : TapToPayGateway {

    override suspend fun collect(
        invoice: Invoice,
        type: PaymentRequestType,
        clientSecret: String,
        paymentIntentId: String,
        stripeAccountId: String
    ): TapToPayOutcome {
        return when (
            val sheet = paymentSheetGateway.presentPaymentSheet(
                invoice = invoice,
                type = type,
                clientSecret = clientSecret,
                paymentIntentId = paymentIntentId,
                stripeAccountId = stripeAccountId
            )
        ) {
            com.fordham.toolbelt.domain.model.stripe.StripeCardCollectOutcome.Cancelled ->
                TapToPayOutcome.Cancelled
            is com.fordham.toolbelt.domain.model.stripe.StripeCardCollectOutcome.Failure ->
                TapToPayOutcome.Failure(sheet.error)
            is com.fordham.toolbelt.domain.model.stripe.StripeCardCollectOutcome.Success ->
                TapToPayOutcome.Success(
                    request = sheet.request.copy(provider = com.fordham.toolbelt.domain.model.PaymentProviderType.TapToPay),
                    paymentIntentId = sheet.paymentIntentId
                )
        }
    }
}
