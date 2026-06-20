package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.UnpaidInvoiceReminderContent
import com.fordham.toolbelt.util.UnpaidInvoiceReminderTemplates
import com.fordham.toolbelt.util.UnpaidInvoiceReminders
import com.fordham.toolbelt.util.UserFacingCopy

/**
 * Builds deterministic localized title/body for unpaid-invoice notifications.
 * Shared by Android WorkManager, iOS local notifications, and reminder sync.
 */
class ComposeUnpaidInvoiceReminderUseCase {
    operator fun invoke(
        invoices: List<Invoice>,
        contractorName: String? = null
    ): UnpaidInvoiceReminderContent? {
        val unpaid = invoices.filter { !it.isPaid && !it.isEstimate }
        if (unpaid.isEmpty()) return null

        val primary = unpaid.maxByOrNull { it.lastUpdated } ?: return null
        val title = if (unpaid.size == 1) {
            UserFacingCopy.Notifications.paymentReminderTitle(primary.clientName)
        } else {
            UnpaidInvoiceReminders.title()
        }
        val body = UnpaidInvoiceReminderTemplates.body(contractorName, unpaid)
        return UnpaidInvoiceReminderContent(title = title, body = body)
    }
}
