package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil

data class Invoice(
    val id: InvoiceId,
    val clientName: String,
    val clientAddress: String,
    val clientPhone: PhoneNumber = PhoneNumber(""),
    val clientEmail: EmailAddress = EmailAddress(""),
    val date: String,
    val totalAmount: Double,
    val depositAmount: Double = 0.0,
    val itemsSummary: String,
    val pdfPath: String = "",
    val isPaid: Boolean = false,
    val isEstimate: Boolean = false,
    val lastUpdated: Long = DateTimeUtil.nowEpochMillis(),
    val durationSeconds: Long = 0L
) {
    val formattedTotal: String get() = DateTimeUtil.formatMoney(totalAmount)
    val formattedDeposit: String get() = "Deposit Paid: ${DateTimeUtil.formatMoney(depositAmount)}"
}
