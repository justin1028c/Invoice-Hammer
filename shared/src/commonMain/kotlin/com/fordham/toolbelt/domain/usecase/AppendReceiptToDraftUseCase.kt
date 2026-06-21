package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.DraftRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.first

class AppendReceiptToDraftUseCase(
    private val draftRepository: DraftRepository,
    private val invoiceRepository: InvoiceRepository,
    private val receiptRepository: ReceiptRepository
) {
    suspend operator fun invoke(
        match: ExpenseMatchResult,
        receiptItems: List<ReceiptItem>,
        markupPercentage: Double = 0.0
    ): Boolean {
        if (receiptItems.isEmpty()) return false

        val newLines = receiptItems.map { item ->
            val priceWithMarkup = item.totalPrice * (1.0 + (markupPercentage / 100.0))
            val description = if (markupPercentage > 0.0) {
                "${item.description} (incl. ${markupPercentage.toInt()}% markup)"
            } else {
                item.description
            }
            LineItem(
                description = ItemsSummary(description),
                amount = MoneyAmount(priceWithMarkup),
                category = "Materials",
                quantity = item.quantity,
                unitPrice = item.unitPrice?.let { MoneyAmount(it) }
            )
        }

        if (match.isEstimate) {
            val estimateId = InvoiceId(match.targetId)
            val invoice = invoiceRepository.getInvoiceById(estimateId) ?: return false
            val newTotal = invoice.totalAmount + MoneyAmount(newLines.map { it.amount.value }.sum())
            val newSummary = if (invoice.itemsSummary.value.isBlank()) {
                receiptItems.joinToString(", ") { it.description }
            } else {
                "${invoice.itemsSummary.value}, " + receiptItems.joinToString(", ") { it.description }
            }

            invoiceRepository.insertInvoice(
                invoice.copy(
                    totalAmount = newTotal,
                    itemsSummary = ItemsSummary(newSummary),
                    lastUpdated = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                )
            )

            receiptItems.forEach { item ->
                receiptRepository.updateItem(
                    item.copy(
                        isBilled = true,
                        linkedInvoiceId = estimateId
                    )
                )
            }
        } else {
            val draft = draftRepository.getDraft().first()
            draftRepository.saveDraft(
                draft.copy(
                    lineItems = draft.lineItems + newLines,
                    linkedReceiptIds = draft.linkedReceiptIds + receiptItems.map { it.id.value }
                )
            )

            receiptItems.forEach { item ->
                receiptRepository.updateItem(
                    item.copy(
                        isBilled = true,
                        linkedInvoiceId = InvoiceId("current_draft")
                    )
                )
            }
        }
        return true
    }
}
