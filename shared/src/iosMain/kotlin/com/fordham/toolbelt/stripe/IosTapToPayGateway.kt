package com.fordham.toolbelt.stripe

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.stripe.TapToPayOutcome
import com.fordham.toolbelt.domain.repository.TapToPayGateway

class IosTapToPayGateway(
    private val paymentSheetGateway: IosStripePaymentSheetGateway
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
                invoice, type, clientSecret, paymentIntentId, stripeAccountId
            )
        ) {
            com.fordham.toolbelt.domain.model.stripe.StripeCardCollectOutcome.Cancelled -> TapToPayOutcome.Cancelled
            is com.fordham.toolbelt.domain.model.stripe.StripeCardCollectOutcome.Failure ->
                TapToPayOutcome.Failure(sheet.error)
            is com.fordham.toolbelt.domain.model.stripe.StripeCardCollectOutcome.Success ->
                TapToPayOutcome.Success(
                    request = sheet.request.copy(provider = PaymentProviderType.TapToPay),
                    paymentIntentId = sheet.paymentIntentId
                )
        }
    }
}
