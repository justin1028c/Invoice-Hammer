package com.fordham.toolbelt.stripe

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.stripe.BluetoothReaderOutcome
import com.fordham.toolbelt.domain.repository.BluetoothCardReaderGateway

/**
 * Pro-tier external reader path. Stripe Terminal Bluetooth discovery lands here next.
 */
class AndroidBluetoothCardReaderGateway : BluetoothCardReaderGateway {
    override suspend fun collect(
        invoice: Invoice,
        type: PaymentRequestType,
        clientSecret: String,
        paymentIntentId: String,
        stripeAccountId: String
    ): BluetoothReaderOutcome = BluetoothReaderOutcome.ReaderNotAvailable
}
