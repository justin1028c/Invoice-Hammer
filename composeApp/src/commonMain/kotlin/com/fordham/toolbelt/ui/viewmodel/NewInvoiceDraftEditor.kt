package com.fordham.toolbelt.ui.viewmodel

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.DraftRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

internal class NewInvoiceDraftEditor(
    private val draftRepository: DraftRepository
) {
    val draft: Flow<DraftInvoice> = draftRepository.getDraft()

    suspend fun currentDraft(): DraftInvoice = draft.first()

    suspend fun updateDraft(update: (DraftInvoice) -> DraftInvoice) {
        draftRepository.saveDraft(update(currentDraft()))
    }

    private var isLoopRunning = false

    suspend fun resumeTimerLoop() {
        if (isLoopRunning) return
        isLoopRunning = true
        try {
            while (true) {
                val draft = currentDraft()
                if (!draft.timerRunning) break

                val elapsedSeconds = (Clock.System.now().toEpochMilliseconds() - draft.startTime) / 1000
                draftRepository.saveDraft(draft.copy(elapsedSeconds = elapsedSeconds))
                delay(1000)
            }
        } finally {
            isLoopRunning = false
        }
    }

    suspend fun toggleTimer() {
        val current = currentDraft()
        if (current.timerRunning) {
            val now = Clock.System.now().toEpochMilliseconds()
            val elapsed = maxOf(0L, (now - current.startTime) / 1000)
            draftRepository.saveDraft(current.copy(timerRunning = false, elapsedSeconds = elapsed))
            return
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val start = if (current.elapsedSeconds > 0) {
            now - (current.elapsedSeconds * 1000)
        } else {
            now
        }
        draftRepository.saveDraft(current.copy(timerRunning = true, startTime = start))
        resumeTimerLoop()
    }

    suspend fun addManualItem(): Boolean {
        val draft = currentDraft()
        val amount = draft.itemAmt.toDoubleOrNull() ?: 0.0
        if (amount <= 0.0 || draft.itemDesc.isBlank()) return false

        val lineItems = draft.lineItems + LineItem(ItemsSummary(draft.itemDesc), MoneyAmount(amount), draft.selectedCategory)
        draftRepository.saveDraft(draft.copy(lineItems = lineItems, itemDesc = "", itemAmt = ""))
        return true
    }

    suspend fun acceptAiItems(items: List<LineItem>) {
        val draft = currentDraft()
        draftRepository.saveDraft(draft.copy(lineItems = draft.lineItems + items, itemDesc = ""))
    }

    suspend fun linkReceipt(receipt: ReceiptItem, markupPercent: Double) {
        val draft = currentDraft()
        val amount = receipt.totalPrice * (1.0 + (markupPercent / 100.0))
        val description = if (markupPercent > 0) {
            "${receipt.description} (incl. ${markupPercent.toInt()}% markup)"
        } else {
            receipt.description
        }

        draftRepository.saveDraft(
            draft.copy(
                lineItems = draft.lineItems + LineItem(ItemsSummary(description), MoneyAmount(amount), "Parts"),
                linkedReceiptIds = draft.linkedReceiptIds + receipt.id.value
            )
        )
    }

    suspend fun clearDraft() {
        draftRepository.clearDraft()
    }

}
