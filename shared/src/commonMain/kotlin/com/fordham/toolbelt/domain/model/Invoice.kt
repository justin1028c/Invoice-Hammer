package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil

data class Invoice(
    val id: InvoiceId,
    val clientName: ClientName,
    val clientAddress: ClientAddress,
    val clientPhone: PhoneNumber = PhoneNumber(""),
    val clientEmail: EmailAddress = EmailAddress(""),
    val date: String, // Date string format is handled by DateTimeUtil parser
    val totalAmount: MoneyAmount,
    val depositAmount: MoneyAmount = MoneyAmount(0.0),
    val itemsSummary: ItemsSummary,
    val pdfPath: PdfFilePath = PdfFilePath(""),
    val isPaid: Boolean = false,
    val isEstimate: Boolean = false,
    val lastUpdated: Long = DateTimeUtil.nowEpochMillis(),
    val durationSeconds: DurationSeconds = DurationSeconds(0L)
) {
    val formattedTotal: String get() = DateTimeUtil.formatMoney(totalAmount.value)
    val formattedDeposit: String get() = DateTimeUtil.formatMoney(depositAmount.value)
}
