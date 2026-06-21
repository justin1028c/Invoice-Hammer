package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.BillLaborFailure
import com.fordham.toolbelt.domain.model.BillLaborOutcome
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.LineItem
import com.fordham.toolbelt.domain.repository.DraftRepository
import kotlin.math.abs
import kotlin.math.round
import kotlinx.coroutines.flow.first

/**
 * Responsibility: Convert elapsed timer seconds into a labor line item and update the draft.
 */
class BillLaborUseCase(
    private val draftRepository: DraftRepository
) {
    suspend operator fun invoke(): BillLaborOutcome = try {
        val draft = draftRepository.getDraft().first()
        val hours = draft.elapsedSeconds / 3600.0
        val rate = draft.hourlyRate
        val item = LineItem(
            description = com.fordham.toolbelt.domain.model.ItemsSummary("Labor: ${formatTwoDecimals(hours)} hours"),
            amount = com.fordham.toolbelt.domain.model.MoneyAmount(hours * rate),
            category = "Labor"
        )
        
        val updatedItems = draft.lineItems + item
        
        draftRepository.saveDraft(draft.copy(
            lineItems = updatedItems,
            elapsedSeconds = 0L,
            timerRunning = false
        ))
        BillLaborOutcome.Success
    } catch (e: Exception) {
        com.fordham.toolbelt.util.AppLogger.e("BillLaborUseCase", "invoke failed", e)
        BillLaborOutcome.Error(
            BillLaborFailure.UnexpectedFailure(FailureMessage(e.message ?: "Failed to bill labor"))
        )
    }

    // Multiplatform-safe two-decimal format (kotlin.text.format is JVM-only and
    // is not available in commonMain for iosX64/iosArm64/iosSimulatorArm64).
    private fun formatTwoDecimals(value: Double): String {
        val scaled = round(value * 100.0).toLong()
        val sign = if (scaled < 0L) "-" else ""
        val absScaled = abs(scaled)
        val whole = absScaled / 100L
        val fraction = (absScaled % 100L).toString().padStart(2, '0')
        return "$sign$whole.$fraction"
    }
}
