package com.fordham.toolbelt.domain.model.stripe

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import kotlin.jvm.JvmInline

@JvmInline
value class StripePaymentIntentId(val value: String)

sealed interface StripeCardCollectOutcome {
    data class Success(
        val request: InvoicePaymentRequest,
        val paymentIntentId: StripePaymentIntentId
    ) : StripeCardCollectOutcome

    data object Cancelled : StripeCardCollectOutcome
    data class Failure(val error: FailureMessage) : StripeCardCollectOutcome
}

sealed interface TapToPayOutcome {
    data class Success(
        val request: InvoicePaymentRequest,
        val paymentIntentId: StripePaymentIntentId
    ) : TapToPayOutcome

    data object Cancelled : TapToPayOutcome
    data class Failure(val error: FailureMessage) : TapToPayOutcome
}

sealed interface BluetoothReaderOutcome {
    data class Success(val request: InvoicePaymentRequest) : BluetoothReaderOutcome
    data object Cancelled : BluetoothReaderOutcome
    data object PremiumRequired : BluetoothReaderOutcome
    /** Physical Bluetooth reader path is not enabled in this build (same on Android and iOS). */
    data object ReaderNotAvailable : BluetoothReaderOutcome
    data class Failure(val error: FailureMessage) : BluetoothReaderOutcome
}

enum class StripePaymentMode {
    /** PCI-safe Payment Sheet (publishable key + backend intent). */
    PaymentSheet,
    /** On-device beta simulator when Stripe backend is not configured. */
    ManualEntrySimulator
}
