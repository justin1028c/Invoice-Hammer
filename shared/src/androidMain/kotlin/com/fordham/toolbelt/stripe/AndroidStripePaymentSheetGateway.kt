package com.fordham.toolbelt.stripe

import com.fordham.toolbelt.billing.BillingActivityHolder
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.FailureMessage
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
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidStripePaymentSheetGateway : StripePaymentSheetGateway {

    override suspend fun presentPaymentSheet(
        invoice: Invoice,
        type: PaymentRequestType,
        clientSecret: String,
        paymentIntentId: String,
        stripeAccountId: String
    ): StripeCardCollectOutcome {
        if (BillingActivityHolder.activity == null) {
            return StripeCardCollectOutcome.Failure(
                FailureMessage("Payment screen is not ready. Reopen the app and try again.")
            )
        }

        return suspendCancellableCoroutine { cont ->
            StripePaymentSheetCoordinator.present(
                clientSecret,
                stripeAccountId,
                object : kotlin.coroutines.Continuation<PaymentSheetResult> {
                    override val context = cont.context
                    override fun resumeWith(result: Result<PaymentSheetResult>) {
                        val sheetResult = result.getOrElse { error ->
                            cont.resume(
                                StripeCardCollectOutcome.Failure(
                                    FailureMessage(error.message ?: "Stripe payment failed.")
                                )
                            )
                            return
                        }
                        if (cont.isActive) {
                            cont.resume(mapResult(invoice, type, paymentIntentId, sheetResult))
                        }
                    }
                }
            )
        }
    }

    private fun mapResult(
        invoice: Invoice,
        type: PaymentRequestType,
        paymentIntentId: String,
        result: PaymentSheetResult
    ): StripeCardCollectOutcome = when (result) {
        PaymentSheetResult.Canceled -> StripeCardCollectOutcome.Cancelled
        is PaymentSheetResult.Failed ->
            StripeCardCollectOutcome.Failure(
                FailureMessage(result.error.localizedMessage ?: "Stripe payment failed.")
            )
        PaymentSheetResult.Completed -> {
            val amount = when (type) {
                PaymentRequestType.Deposit ->
                    invoice.depositAmount.takeIf { it > 0.0 } ?: invoice.totalAmount * 0.30
                PaymentRequestType.FullBalance -> invoice.totalAmount
            }
            StripeCardCollectOutcome.Success(
                request = InvoicePaymentRequest(
                    id = PaymentRequestId(randomUUID()),
                    invoiceId = invoice.id,
                    invoiceClientName = invoice.clientName,
                    type = type,
                    provider = PaymentProviderType.CardTerminal,
                    requestedAmount = MoneyAmount(amount),
                    status = InvoicePaymentStatus.Paid,
                    paymentLink = PaymentLinkUrl("stripe://payment_intent/$paymentIntentId"),
                    paidAtMillis = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                    assetCode = "USD"
                ),
                paymentIntentId = StripePaymentIntentId(paymentIntentId)
            )
        }
    }
}
