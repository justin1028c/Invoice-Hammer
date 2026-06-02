package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.PowerPayPaymentResponseDto
import com.fordham.toolbelt.data.remote.PowerPayWebhookEventDto
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.domain.model.PaymentReceiptUrl
import com.fordham.toolbelt.domain.model.PowerPayEvent
import com.fordham.toolbelt.domain.model.PowerPayEventId
import com.fordham.toolbelt.domain.model.PowerPayMilestoneId
import com.fordham.toolbelt.domain.model.PowerPayProjectId
import com.fordham.toolbelt.domain.model.StellarExplorerUrl
import com.fordham.toolbelt.domain.model.StellarTransactionHash
import com.fordham.toolbelt.util.randomUUID

internal object PowerPayEventMapper {
    fun fromWebhookDto(dto: PowerPayWebhookEventDto): PowerPayEvent? = fromType(
        type = dto.type,
        eventId = dto.eventId ?: randomUUID(),
        invoiceId = dto.invoiceId,
        projectId = dto.projectId,
        milestoneId = dto.milestoneId,
        amount = dto.amount,
        receiptUrl = dto.receiptUrl,
        transactionHash = dto.transactionHash,
        explorerUrl = dto.explorerUrl
    )

    fun fromPaidPayment(dto: PowerPayPaymentResponseDto): PowerPayEvent.InvoicePaid? {
        if (!isPaidStatus(dto.status)) return null
        return PowerPayEvent.InvoicePaid(
            eventId = PowerPayEventId("payment-${dto.paymentId}"),
            invoiceId = InvoiceId(dto.invoiceId),
            receiptUrl = dto.receiptUrl?.let { PaymentReceiptUrl(it) }
                ?: dto.paymentLinkUrl.takeIf { it.isNotBlank() }?.let { PaymentReceiptUrl(it) },
            transactionHash = dto.transactionHash?.let { StellarTransactionHash(it) },
            explorerUrl = dto.explorerUrl?.let { StellarExplorerUrl(it) }
        )
    }

    private fun isPaidStatus(raw: String): Boolean = when (raw.lowercase()) {
        "paid", "paid_in_full", "deposit_paid", "milestone_paid", "completed", "succeeded" -> true
        else -> false
    }

    private fun fromType(
        type: String,
        eventId: String,
        invoiceId: String?,
        projectId: String?,
        milestoneId: String?,
        amount: Double?,
        receiptUrl: String?,
        transactionHash: String?,
        explorerUrl: String?
    ): PowerPayEvent? {
        val id = PowerPayEventId(eventId)
        return when (type.lowercase().replace('_', '.')) {
            "invoice.paid" -> PowerPayEvent.InvoicePaid(
                eventId = id,
                invoiceId = InvoiceId(invoiceId ?: return null),
                receiptUrl = receiptUrl?.let { PaymentReceiptUrl(it) },
                transactionHash = transactionHash?.let { StellarTransactionHash(it) },
                explorerUrl = explorerUrl?.let { StellarExplorerUrl(it) }
            )
            "deposit.received" -> PowerPayEvent.DepositReceived(
                eventId = id,
                invoiceId = invoiceId?.let { InvoiceId(it) },
                projectId = projectId?.let { PowerPayProjectId(it) },
                amount = amount?.let { MoneyAmount(it) }
            )
            "milestone.released" -> PowerPayEvent.MilestoneReleased(
                eventId = id,
                invoiceId = invoiceId?.let { InvoiceId(it) },
                milestoneId = PowerPayMilestoneId(milestoneId ?: return null),
                amount = amount?.let { MoneyAmount(it) }
            )
            else -> null
        }
    }
}
