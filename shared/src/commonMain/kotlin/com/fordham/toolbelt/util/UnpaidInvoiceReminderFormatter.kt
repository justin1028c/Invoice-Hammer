package com.fordham.toolbelt.util

import com.fordham.toolbelt.domain.model.Invoice

/**
 * Shared copy for unpaid-invoice reminder notifications (Android WorkManager + iOS local notifications).
 */
object UnpaidInvoiceReminderFormatter {
    fun formatBody(invoices: List<Invoice>): String {
        val unpaid = invoices
            .filter { !it.isPaid && !it.isEstimate }
            .sortedByDescending { it.lastUpdated }
            .take(3)
        return if (unpaid.isEmpty()) {
            UnpaidInvoiceReminders.BODY
        } else {
            unpaid.joinToString("\n") { "${it.clientName}: ${it.formattedTotal}" }
        }
    }
}
