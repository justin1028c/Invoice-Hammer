package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil
import kotlinx.datetime.Clock
import kotlin.jvm.JvmInline

@JvmInline
value class PaymentRequestId(val value: String)

@JvmInline
value class PaymentLinkUrl(val value: String)

@JvmInline
value class StellarTransactionHash(val value: String)

enum class PaymentRequestType {
    Deposit,
    FullBalance
}

enum class PaymentProviderType {
    GooglePay,
    ApplePay,
    StellarUsdc,
    CardLink
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
    val paidAtMillis: Long? = null,
    val stellarTransactionHash: StellarTransactionHash? = null,
    val assetCode: String = "USDC"
) {
    val formattedAmount: String get() = DateTimeUtil.formatMoney(requestedAmount.value)
    val statusLabel: String get() = status.name.uppercase()
    val providerLabel: String get() = provider.label
}

val PaymentProviderType.label: String
    get() = when (this) {
        PaymentProviderType.GooglePay -> "Google Pay"
        PaymentProviderType.ApplePay -> "Apple Pay"
        PaymentProviderType.StellarUsdc -> "Stellar USDC"
        PaymentProviderType.CardLink -> "Card / Link"
    }

sealed interface PaymentRequestOutcome {
    data class Success(val request: InvoicePaymentRequest) : PaymentRequestOutcome
    data class Failure(val error: FailureMessage) : PaymentRequestOutcome
}

sealed interface PaymentLedgerOutcome {
    data class Success(val requests: List<InvoicePaymentRequest>) : PaymentLedgerOutcome
    data class Failure(val error: FailureMessage) : PaymentLedgerOutcome
}
