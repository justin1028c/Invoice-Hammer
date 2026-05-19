package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil

data class JobNote(
    val id: NoteId,
    val clientName: String = "",
    val invoiceId: InvoiceId? = null,
    val text: String,
    val timestamp: Long = DateTimeUtil.nowEpochMillis()
) {
    val formattedTime: String get() = DateTimeUtil.formatEpoch(timestamp)
    val formattedDate: String get() = DateTimeUtil.formatEpoch(timestamp)
}
