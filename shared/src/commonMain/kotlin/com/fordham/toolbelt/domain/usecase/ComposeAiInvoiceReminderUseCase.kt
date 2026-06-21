package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase

enum class ReminderTone {
    FRIENDLY,
    DIRECT,
    FIRM
}

enum class ReminderChannel {
    SMS,
    EMAIL
}

class ComposeAiInvoiceReminderUseCase(
    private val geminiRepository: GeminiRepository,
    private val hasSubscriptionFeature: HasSubscriptionFeatureUseCase
) {
    suspend operator fun invoke(
        invoice: Invoice,
        tone: ReminderTone,
        channel: ReminderChannel,
        contractorName: String?,
        paymentLink: String?
    ): GeminiOutcome {
        if (!hasSubscriptionFeature(SubscriptionFeature.AiAgent)) {
            return GeminiOutcome.Failure(
                FailureMessage("Pro subscription required for AI reminder generation.")
            )
        }

        val toneInstruction = when (tone) {
            ReminderTone.FRIENDLY -> "Use a polite, friendly, and helpful tone. Keep it warm but professional."
            ReminderTone.DIRECT -> "Use a direct, assertive, and professional tone. Be clear and straight to the point."
            ReminderTone.FIRM -> "Use a firm and serious tone, highlighting that the payment is overdue and prompt payment is expected."
        }

        val channelInstruction = when (channel) {
            ReminderChannel.SMS -> "Optimize this for SMS. The body should be very short, brief, and punchy (maximum 150-180 characters). The subject should be a short prefix like 'Reminder'."
            ReminderChannel.EMAIL -> "Optimize this for Email. Provide a clear, professional subject line, and a standard email body structure (2-3 sentences max)."
        }

        val promptText = buildString {
            append("Compose a payment reminder message for the following invoice:\n")
            append("Client Name: ${invoice.clientName.value}\n")
            append("Invoice ID: ${invoice.id.value}\n")
            append("Invoice Date: ${invoice.date}\n")
            append("Total Amount: \$${invoice.totalAmount.value}\n")
            if (invoice.depositAmount.value > 0.0) {
                append("Deposit Paid: \$${invoice.depositAmount.value}\n")
            }
            append("Items: ${invoice.itemsSummary.value}\n")
            if (!contractorName.isNullOrBlank()) {
                append("Contractor/Sender Name: $contractorName\n")
            }
            if (!paymentLink.isNullOrBlank()) {
                append("Payment Link: $paymentLink\n")
            }
            append("\n")
            append("TONE REQUIREMENT: $toneInstruction\n")
            append("CHANNEL REQUIREMENT: $channelInstruction\n")
        }

        return geminiRepository.processTask(TaskType.GENERATE, promptText)
    }
}
