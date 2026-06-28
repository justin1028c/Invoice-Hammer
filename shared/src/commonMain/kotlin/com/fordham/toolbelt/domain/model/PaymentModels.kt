package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil
import kotlinx.datetime.Clock
import kotlin.jvm.JvmInline

@JvmInline
value class PaymentRequestId(val value: String)

@JvmInline
value class PaymentLinkUrl(val value: String)

enum class PaymentRequestType {
    Deposit,
    FullBalance
}

enum class PaymentProviderType {
    GooglePay,
    ApplePay,
    CardLink,
    CardTerminal,
    TapToPay,
    BluetoothReader
}

enum class InvoicePaymentStatus {
    Requested,
    Pending,
    Paid,
    Failed,
    Expired
}

data class InvoicePaymentRequest(
    val id: PaymentRequestId,
    val invoiceId: InvoiceId,
    val invoiceClientName: String,
    val type: PaymentRequestType,
    val provider: PaymentProviderType,
    val requestedAmount: MoneyAmount,
    val status: InvoicePaymentStatus,
    val paymentLink: PaymentLinkUrl,
    val createdAtMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val paidAtMillis: Long? = null
) {
    val formattedAmount: String get() = DateTimeUtil.formatMoney(requestedAmount.value)
    val statusLabel: String get() = status.name.uppercase()
    val providerLabel: String get() = provider.label
}

val PaymentProviderType.label: String
    get() = when (this) {
        PaymentProviderType.GooglePay -> "Google Pay"
        PaymentProviderType.ApplePay -> "Apple Pay"
        PaymentProviderType.CardLink -> "Card / Link"
        PaymentProviderType.CardTerminal -> "Card Terminal"
        PaymentProviderType.TapToPay -> "Tap to Pay"
        PaymentProviderType.BluetoothReader -> "Bluetooth Reader (Pro)"
    }

/** Stripe Connect hosted checkout (card link, Google Pay, Apple Pay). */
val PaymentProviderType.usesStripeRail: Boolean
    get() = when (this) {
        PaymentProviderType.GooglePay,
        PaymentProviderType.ApplePay,
        PaymentProviderType.CardLink -> true
        else -> false
    }

/** On-site collection — not created via payment-link flow. */
val PaymentProviderType.isOnSiteCollection: Boolean
    get() = when (this) {
        PaymentProviderType.CardTerminal,
        PaymentProviderType.TapToPay,
        PaymentProviderType.BluetoothReader -> true
        else -> false
    }

fun PaymentProviderType.checkoutInstructions(
    isStripeLive: Boolean
): String = when (this) {
    PaymentProviderType.GooglePay ->
        if (isStripeLive) {
            "Stripe Connect checkout with Google Pay enabled. Share this link with your client."
        } else {
            "Demo Google Pay link — add stripe.publishable.key and stripe.payment.backend.url for live checkout."
        }
    PaymentProviderType.ApplePay ->
        if (isStripeLive) {
            "Stripe Connect checkout with Apple Pay enabled. Share this link with your client (iOS/Safari)."
        } else {
            "Demo Apple Pay link — add Stripe keys and payment backend for live checkout."
        }
    PaymentProviderType.CardLink ->
        if (isStripeLive) {
            "Stripe hosted card / Link checkout. PCI-safe — card data stays on Stripe."
        } else {
            "Demo card link — add Stripe keys and payment backend for live hosted checkout."
        }
    else -> "Open the link to complete payment."
}

sealed interface PaymentRequestOutcome {
    data class Success(val request: InvoicePaymentRequest) : PaymentRequestOutcome
    data class Failure(
        val error: FailureMessage,
        val actionUrl: String? = null
    ) : PaymentRequestOutcome
}

sealed interface PaymentLedgerOutcome {
    data class Success(val requests: List<InvoicePaymentRequest>) : PaymentLedgerOutcome
    data class Failure(val error: FailureMessage) : PaymentLedgerOutcome
}
