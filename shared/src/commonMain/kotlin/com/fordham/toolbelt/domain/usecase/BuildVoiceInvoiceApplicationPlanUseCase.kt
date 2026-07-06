package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.AiInvoiceResult
import com.fordham.toolbelt.domain.model.DraftInvoice
import com.fordham.toolbelt.domain.model.VoiceInvoiceApplicationPlan
import com.fordham.toolbelt.util.AppLogger

class BuildVoiceInvoiceApplicationPlanUseCase {
    operator fun invoke(
        draft: DraftInvoice,
        ai: AiInvoiceResult
    ): VoiceInvoiceApplicationPlan {
        val issues = ai.validationIssues.distinct()
        val lowConfidence = ai.confidenceScore < MIN_AUTO_APPLY_CONFIDENCE ||
            "LOW_AUDIO_CONFIDENCE" in issues
        val blockedForFollowUp = lowConfidence ||
            (ai.clientName.isBlank() && draft.clientName.isBlank()) ||
            ("ZERO_AMOUNT" in issues && ai.items.isEmpty())
        AppLogger.d(
            LOG_TAG,
            "PLAN_INPUT draftClient='${draft.clientName}' draftAddress='${draft.clientAddress}' " +
                "aiClient='${ai.clientName}' aiAddress='${ai.clientAddress}' items=${ai.items.size} " +
                "confidence=${ai.confidenceScore} issues=$issues blocked=$blockedForFollowUp"
        )

        if (blockedForFollowUp) {
            AppLogger.d(LOG_TAG, "PLAN_BLOCKED requiresFollowUp=true pendingItems=0")
            return VoiceInvoiceApplicationPlan(
                laborHours = ai.laborHours,
                laborRate = ai.laborRate,
                discountPercent = ai.discountPercent,
                notes = ai.notes,
                confidenceScore = ai.confidenceScore,
                userSummary = ai.userSummary,
                validationIssues = issues,
                requiresFollowUp = true
            )
        }

        return VoiceInvoiceApplicationPlan(
            clientName = ai.clientName.takeIf { it.isNotBlank() },
            clientAddress = ai.clientAddress.takeIf { it.isNotBlank() },
            taxRatePercent = ai.taxRatePercent.takeIf { it > 0.0 },
            depositAmount = ai.depositAmount.takeIf { it > 0.0 },
            hourlyRate = ai.laborRate,
            pendingLineItems = ai.items,
            laborHours = ai.laborHours,
            laborRate = ai.laborRate,
            discountPercent = ai.discountPercent,
            notes = ai.notes,
            confidenceScore = ai.confidenceScore,
            userSummary = ai.userSummary,
            validationIssues = issues,
            requiresFollowUp = issues.isNotEmpty() && ai.items.isEmpty()
        )
            .also {
                AppLogger.d(
                    LOG_TAG,
                    "PLAN_READY client='${it.clientName.orEmpty()}' address='${it.clientAddress.orEmpty()}' " +
                        "pendingItems=${it.pendingLineItems.size} requiresFollowUp=${it.requiresFollowUp} " +
                        "lines=${it.pendingLineItems.joinToString(" | ") { item -> "${item.description.value}:${item.amount.value}:qty=${item.quantity}:unit=${item.unitPrice?.value}:cat=${item.category}" }}"
                )
            }
    }

    private companion object {
        const val LOG_TAG = "VoiceInvoicePipeline"
        const val MIN_AUTO_APPLY_CONFIDENCE = 0.60
    }
}
