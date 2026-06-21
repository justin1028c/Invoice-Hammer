package com.fordham.toolbelt.domain.usecase.agent

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.JobNoteRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.first

sealed interface ProfitGuardianOutcome {
    data class Success(val status: ProfitGuardianStatus) : ProfitGuardianOutcome
    data class ProjectNotFound(val invoiceId: InvoiceId) : ProfitGuardianOutcome
    data class Error(val message: FailureMessage) : ProfitGuardianOutcome
}

class GetProfitGuardianStatusUseCase(
    private val invoiceRepository: InvoiceRepository,
    private val receiptRepository: ReceiptRepository,
    private val jobNoteRepository: JobNoteRepository
) {
    suspend operator fun invoke(invoiceId: InvoiceId): ProfitGuardianOutcome = try {
        val invoice = invoiceRepository.getInvoiceById(invoiceId)
            ?: return ProfitGuardianOutcome.ProjectNotFound(invoiceId)

        // 1. Fetch system budget note linked to this invoice
        val notes = jobNoteRepository.getNotesByInvoice(invoiceId).first()
        val budgetNote = notes.firstOrNull { it.text.startsWith("[SYSTEM_BUDGET]") }
        
        val parsedBudget = budgetNote?.let { SystemBudgetSerializer.deserialize(it.text) }

        val budgetedRevenue = parsedBudget?.revenue ?: invoice.totalAmount.value
        val budgetedMaterials = parsedBudget?.materials ?: 0.0

        // 2. Fetch actually linked receipt expenses
        val receiptsOutcome = receiptRepository.allItems.first()
        val allReceipts = (receiptsOutcome as? ReceiptListOutcome.Success)?.receipts ?: emptyList()
        val linkedReceipts = allReceipts.filter { it.linkedInvoiceId == invoiceId }
        val actualMaterials = linkedReceipts.map { it.totalPrice }.sum()

        val materialVariance = actualMaterials - budgetedMaterials

        // Material Profit Guardian V1 Profit calculations:
        val projectedProfit = budgetedRevenue - budgetedMaterials
        val currentProjection = budgetedRevenue - actualMaterials

        val reasons = mutableListOf<NaturalLanguage>()
        val recommendations = mutableListOf<NaturalLanguage>()

        if (materialVariance > 0.0) {
            val percent = if (budgetedMaterials > 0.0) (materialVariance / budgetedMaterials) * 100.0 else 100.0
            reasons.add(NaturalLanguage("Material costs are ${formatPercentage(percent)}% higher than estimated."))
            recommendations.add(NaturalLanguage("Review linked receipts and generate a change order to recover the difference."))
        }

        if (actualMaterials > budgetedMaterials && budgetedMaterials > 0.0) {
            val variancePct = (materialVariance / budgetedMaterials) * 100.0
            if (variancePct >= 10.0) {
                reasons.add(NaturalLanguage("Material budget overrun detected (+$${formatMoney(materialVariance)})."))
            }
        }

        ProfitGuardianOutcome.Success(
            ProfitGuardianStatus(
                invoiceId = invoiceId,
                clientName = invoice.clientName,
                budgetedRevenue = MoneyAmount(budgetedRevenue),
                projectedRevenue = invoice.totalAmount,
                budgetedMaterials = MoneyAmount(budgetedMaterials),
                actualMaterials = MoneyAmount(actualMaterials),
                materialVariance = MoneyVariance(materialVariance),
                projectedProfit = MoneyAmount(projectedProfit.coerceAtLeast(0.0)),
                currentProjection = MoneyAmount(currentProjection.coerceAtLeast(0.0)),
                reasons = reasons,
                recommendations = recommendations
            )
        )
    } catch (e: Exception) {
        com.fordham.toolbelt.util.AppLogger.e("GetProfitGuardianStatusUseCase", "Failed to compute profit guardian status", e)
        ProfitGuardianOutcome.Error(FailureMessage(e.message ?: "Unexpected error calculating profit variance."))
    }

    private fun formatPercentage(value: Double): String {
        return kotlin.math.round(value * 10.0).let { (it / 10.0).toString() }
    }

    private fun formatMoney(value: Double): String {
        return kotlin.math.round(value * 100.0).let { (it / 100.0).toString() }
    }
}
