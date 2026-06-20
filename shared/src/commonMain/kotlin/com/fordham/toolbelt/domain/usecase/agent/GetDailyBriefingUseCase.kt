package com.fordham.toolbelt.domain.usecase.agent

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

sealed interface GetDailyBriefingOutcome {
    data class Success(val briefing: DailyBriefing) : GetDailyBriefingOutcome
    data class Error(val message: FailureMessage) : GetDailyBriefingOutcome
}

class GetDailyBriefingUseCase(
    private val invoiceRepository: InvoiceRepository,
    private val getProfitGuardianStatus: GetProfitGuardianStatusUseCase,
    private val detectChangeOrders: DetectChangeOrdersUseCase
) {
    suspend fun execute(): GetDailyBriefingOutcome = try {
        val allInvoices = invoiceRepository.allInvoices.first()
        val unpaidInvoices = allInvoices.filter { !it.isEstimate && !it.isPaid }

        val overdueCount = unpaidInvoices.size
        val totalOverdue = unpaidInvoices.sumOf { it.totalAmount }

        // Find active estimates (last 60 days) to run profit analysis on
        val now = Clock.System.now().toEpochMilliseconds()
        val activeEstimateJobs = allInvoices.filter { 
            it.isEstimate && (now - it.lastUpdated) < 60L * 24L * 60L * 60L * 1000L 
        }

        val budgetOverruns = mutableListOf<ProfitGuardianStatus>()
        val unbilledOpportunities = mutableListOf<ChangeOrderOpportunity>()

        for (estimate in activeEstimateJobs) {
            // Check Profit Guardian Status
            val profitOutcome = getProfitGuardianStatus(estimate.id)
            if (profitOutcome is ProfitGuardianOutcome.Success) {
                if (profitOutcome.status.isTrendingNegative) {
                    budgetOverruns.add(profitOutcome.status)
                }
            }

            // Check Change Order Opportunities
            val changeOutcome = detectChangeOrders(estimate.id)
            if (changeOutcome is DetectChangeOrdersOutcome.Success) {
                val highConf = changeOutcome.opportunities.filter { 
                    it.confidence == OpportunityConfidence.HIGH || it.confidence == OpportunityConfidence.VERY_HIGH 
                }
                unbilledOpportunities.addAll(highConf)
            }
        }

        // Determine potential profit recovery amount
        val potentialOpportunityRecovery = unbilledOpportunities.sumOf { it.estimatedValueRange.start }
        val potentialOverrunsRecovery = budgetOverruns.sumOf { it.materialVariance.value.coerceAtLeast(0.0) }
        val totalRecovery = potentialOpportunityRecovery + potentialOverrunsRecovery

        // Rank actions to pick the single highest impact RecommendedAction
        val candidateActions = mutableListOf<RecommendedAction>()

        // 1. Unpaid invoice reminders (highest impact is maximum single overdue balance)
        unpaidInvoices.maxByOrNull { it.totalAmount }?.let { overdueInvoice ->
            candidateActions.add(
                RecommendedAction(
                    title = NaturalLanguage("Send invoice reminder to ${overdueInvoice.clientName}"),
                    reason = NaturalLanguage("Invoice has been unpaid with a balance of $${formatMoney(overdueInvoice.totalAmount)}."),
                    estimatedImpact = MoneyAmount(overdueInvoice.totalAmount)
                )
            )
        }

        // 2. High-confidence Change Orders (highest impact is maximum detected task price range start)
        unbilledOpportunities.maxByOrNull { it.estimatedValueRange.start }?.let { opportunity ->
            val startVal = opportunity.estimatedValueRange.start
            candidateActions.add(
                RecommendedAction(
                    title = NaturalLanguage("Create change order for ${opportunity.clientName.value}"),
                    reason = NaturalLanguage("Unbilled work detected: \"${opportunity.detectedTask.value}\"."),
                    estimatedImpact = MoneyAmount(startVal)
                )
            )
        }

        // 3. Material budget warning mitigation actions
        budgetOverruns.maxByOrNull { it.materialVariance.value }?.let { overrun ->
            candidateActions.add(
                RecommendedAction(
                    title = NaturalLanguage("Review material costs on ${overrun.clientName.value}'s project"),
                    reason = NaturalLanguage("Materials are $${formatMoney(overrun.materialVariance.value)} over estimated budget."),
                    estimatedImpact = MoneyAmount(overrun.materialVariance.value.coerceAtLeast(0.0))
                )
            )
        }

        val primaryAction = candidateActions.maxByOrNull { it.estimatedImpact.value }

        GetDailyBriefingOutcome.Success(
            DailyBriefing(
                timestamp = now,
                overdueInvoiceCount = overdueCount,
                totalOverdueAmount = MoneyAmount(totalOverdue),
                budgetOverruns = budgetOverruns,
                unbilledOpportunities = unbilledOpportunities,
                potentialProfitRecovery = MoneyAmount(totalRecovery),
                primaryAction = primaryAction
            )
        )
    } catch (e: Exception) {
        com.fordham.toolbelt.util.AppLogger.e("GetDailyBriefingUseCase", "Briefing execution failed", e)
        GetDailyBriefingOutcome.Error(FailureMessage(e.message ?: "Failed to compile Daily Briefing."))
    }

    private fun formatMoney(value: Double): String {
        return kotlin.math.round(value * 100.0).let { (it / 100.0).toString() }
    }
}
