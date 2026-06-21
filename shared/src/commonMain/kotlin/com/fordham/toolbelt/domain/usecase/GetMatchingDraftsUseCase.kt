package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.DraftRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlin.math.abs

class GetMatchingDraftsUseCase(
    private val draftRepository: DraftRepository,
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(
        receiptItems: List<ReceiptItem>,
        selectedClient: Client?
    ): ExpenseMatchResult? {
        if (receiptItems.isEmpty()) return null

        val draft = draftRepository.getDraft().first()
        val draftClientName = draft.clientName.trim()
        val targetClient = selectedClient?.name?.value ?: receiptItems.firstOrNull { it.clientName.isNotBlank() }?.clientName

        // 1. Check current composing draft
        if (draftClientName.isNotBlank()) {
            val isClientMatch = targetClient != null && draftClientName.equals(targetClient, ignoreCase = true)
            val isCategoryMatch = receiptItems.any { item ->
                item.category.isNotBlank() && item.category.equals(draft.selectedCategory, ignoreCase = true)
            }
            if (isClientMatch || isCategoryMatch) {
                return ExpenseMatchResult(
                    targetId = "current_draft",
                    clientName = draftClientName,
                    category = draft.selectedCategory,
                    isEstimate = false,
                    totalAmount = receiptItems.map { it.totalPrice }.sum()
                )
            }
        }

        // 2. Check recent unpaid estimates (+/- 3 days window)
        val now = Clock.System.now().toEpochMilliseconds()
        val threeDaysMs = 3 * 24 * 3600 * 1000L
        val unpaidEstimates = invoiceRepository.allInvoices.first().filter { 
            it.isEstimate && !it.isPaid && abs(it.lastUpdated - now) <= threeDaysMs
        }

        for (estimate in unpaidEstimates) {
            val isClientMatch = targetClient != null && estimate.clientName.value.equals(targetClient, ignoreCase = true)
            val isCategoryMatch = receiptItems.any { item ->
                estimate.itemsSummary.value.contains(item.category, ignoreCase = true) ||
                estimate.itemsSummary.value.contains(item.description, ignoreCase = true)
            }
            if (isClientMatch || isCategoryMatch) {
                return ExpenseMatchResult(
                    targetId = estimate.id.value,
                    clientName = estimate.clientName.value,
                    category = "Estimate",
                    isEstimate = true,
                    totalAmount = receiptItems.map { it.totalPrice }.sum()
                )
            }
        }

        return null
    }
}
