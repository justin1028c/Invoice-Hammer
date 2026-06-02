package com.fordham.toolbelt.domain.model

import kotlin.jvm.JvmInline

/** Client-side PowerPay events (maps to pay.on(...) in the JS SDK). */
sealed interface PowerPayEvent {
    val eventId: PowerPayEventId

    data class InvoicePaid(
        override val eventId: PowerPayEventId,
        val invoiceId: InvoiceId,
        val receiptUrl: PaymentReceiptUrl?,
        val transactionHash: StellarTransactionHash?,
        val explorerUrl: StellarExplorerUrl?
    ) : PowerPayEvent

    data class DepositReceived(
        override val eventId: PowerPayEventId,
        val invoiceId: InvoiceId?,
        val projectId: PowerPayProjectId?,
        val amount: MoneyAmount?
    ) : PowerPayEvent

    data class MilestoneReleased(
        override val eventId: PowerPayEventId,
        val invoiceId: InvoiceId?,
        val milestoneId: PowerPayMilestoneId,
        val amount: MoneyAmount?
    ) : PowerPayEvent
}

@JvmInline
value class PowerPayEventId(val value: String)

@JvmInline
value class PaymentReceiptUrl(val value: String)

@JvmInline
value class PowerPayProjectId(val value: String)

@JvmInline
value class PowerPayMilestoneId(val value: String)

sealed interface PowerPayEventHandleOutcome {
    data object Handled : PowerPayEventHandleOutcome
    data class Ignored(val reason: String) : PowerPayEventHandleOutcome
    data class Failure(val error: FailureMessage) : PowerPayEventHandleOutcome
}
