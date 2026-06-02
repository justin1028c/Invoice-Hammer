package com.fordham.toolbelt.stripe

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.InvoicePaymentStatus
import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.domain.model.PaymentLinkUrl
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestId
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.stripe.StripeCardCollectOutcome
import com.fordham.toolbelt.domain.model.stripe.StripePaymentIntentId
import com.fordham.toolbelt.domain.repository.StripePaymentSheetGateway
import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

class IosStripePaymentSheetGateway : StripePaymentSheetGateway {

    override suspend fun presentPaymentSheet(
        invoice: Invoice,
        type: PaymentRequestType,
        clientSecret: String,
        paymentIntentId: String,
        stripeAccountId: String
    ): StripeCardCollectOutcome {
        val bridge = IosStripePaymentBridgeProvider.bridge
            ?: return StripeCardCollectOutcome.Failure(
                FailureMessage("Stripe PaymentSheet bridge is not registered on iOS.")
            )

        val native = bridge.presentPaymentSheet(
            clientSecret = clientSecret,
            stripeAccountId = stripeAccountId
        )
        if (!native.success) {
            return if (native.cancelled) {
                StripeCardCollectOutcome.Cancelled
            } else {
                StripeCardCollectOutcome.Failure(FailureMessage(native.errorMessage ?: "Payment failed."))
            }
        }

        val amount = when (type) {
            PaymentRequestType.Deposit ->
                invoice.depositAmount.takeIf { it > 0.0 } ?: invoice.totalAmount * 0.30
            PaymentRequestType.FullBalance -> invoice.totalAmount
        }

        return StripeCardCollectOutcome.Success(
            request = InvoicePaymentRequest(
                id = PaymentRequestId(randomUUID()),
                invoiceId = invoice.id,
                invoiceClientName = invoice.clientName,
                type = type,
                provider = PaymentProviderType.CardTerminal,
                requestedAmount = MoneyAmount(amount),
                status = InvoicePaymentStatus.Paid,
                paymentLink = PaymentLinkUrl("stripe://payment_intent/$paymentIntentId"),
                paidAtMillis = Clock.System.now().toEpochMilliseconds(),
                assetCode = "USD"
            ),
            paymentIntentId = StripePaymentIntentId(paymentIntentId)
        )
    }
}
