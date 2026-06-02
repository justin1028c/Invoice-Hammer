package com.fordham.toolbelt.domain.model.stripe

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.PaymentRequestType

enum class StripePaymentChannel(val wireName: String) {
    CardTerminal("card_terminal"),
    TapToPay("tap_to_pay"),
    BluetoothReader("bluetooth_reader")
}

data class CreateStripePaymentIntentCommand(
    val amountCents: Long,
    val invoiceId: InvoiceId,
    val contractorUserId: String,
    val clientName: String,
    val requestType: PaymentRequestType,
    val channel: StripePaymentChannel
)

sealed interface CreateStripePaymentIntentOutcome {
    data class Ready(
        val clientSecret: String,
        val paymentIntentId: String,
        val stripeAccountId: String
    ) : CreateStripePaymentIntentOutcome

    data object BackendNotConfigured : CreateStripePaymentIntentOutcome
    data class Failure(val error: FailureMessage) : CreateStripePaymentIntentOutcome
}
