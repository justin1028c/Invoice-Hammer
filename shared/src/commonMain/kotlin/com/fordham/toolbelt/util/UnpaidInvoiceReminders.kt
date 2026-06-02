package com.fordham.toolbelt.util

object UnpaidInvoiceReminders {
    const val WORK_ID = "unpaid_invoice_reminder"
    const val TITLE = "Unpaid Invoices Reminder"
    const val BODY = "Tap to review unpaid invoices in Invoice Hammer."
    /** First run delay after enabling reminders (15 minutes). */
    const val INITIAL_DELAY_MS = 15L * 60L * 1000L
    /** Daily repeat interval for iOS local notifications. */
    const val REPEAT_INTERVAL_MS = 24L * 60L * 60L * 1000L
}
