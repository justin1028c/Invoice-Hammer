package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.stripe.BluetoothReaderOutcome

/** External Bluetooth/USB readers — Pro tier. */
interface BluetoothCardReaderGateway {
    suspend fun collect(
        invoice: Invoice,
        type: PaymentRequestType,
        clientSecret: String,
        paymentIntentId: String,
        stripeAccountId: String
    ): BluetoothReaderOutcome
}
