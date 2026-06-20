package com.fordham.toolbelt.util

import com.fordham.toolbelt.domain.model.Invoice

/**
 * Deterministic EN/ES notification bodies for unpaid invoice reminders.
 */
object UnpaidInvoiceReminderTemplates {
    fun body(contractorName: String?, unpaidInvoices: List<Invoice>): String {
        if (unpaidInvoices.isEmpty()) return UnpaidInvoiceReminders.body()
        val sorted = unpaidInvoices.sortedByDescending { it.lastUpdated }
        return if (sorted.size == 1) {
            singleBody(contractorName, sorted.first())
        } else {
            multiBody(contractorName, sorted)
        }
    }

    private fun singleBody(contractorName: String?, invoice: Invoice): String {
        val client = invoice.clientName
        val amount = invoice.formattedTotal
        val invoiceRef = invoice.id.value.take(8).uppercase()
        val contractor = contractorName?.trim()?.takeIf { it.isNotBlank() }
        return if (AppLocale.fromSystem() == AppLocale.Spanish) {
            if (contractor != null) {
                "Hola $contractor, $client le debe $amount por la factura $invoiceRef."
            } else {
                "Recordatorio: $client le debe $amount por la factura $invoiceRef."
            }
        } else {
            if (contractor != null) {
                "Hi $contractor, $client owes you $amount for invoice $invoiceRef."
            } else {
                "Reminder: $client owes you $amount for invoice $invoiceRef."
            }
        }
    }

    private fun multiBody(contractorName: String?, invoices: List<Invoice>): String {
        val top = invoices.take(3)
        val lines = top.joinToString("\n") { "• ${it.clientName}: ${it.formattedTotal}" }
        val contractor = contractorName?.trim()?.takeIf { it.isNotBlank() }
        return if (AppLocale.fromSystem() == AppLocale.Spanish) {
            val header = if (contractor != null) {
                "Hola $contractor, tiene ${invoices.size} factura(s) pendiente(s):"
            } else {
                "Tiene ${invoices.size} factura(s) pendiente(s):"
            }
            "$header\n$lines"
        } else {
            val header = if (contractor != null) {
                "Hi $contractor, you have ${invoices.size} unpaid invoice(s):"
            } else {
                "You have ${invoices.size} unpaid invoice(s):"
            }
            "$header\n$lines"
        }
    }
}
