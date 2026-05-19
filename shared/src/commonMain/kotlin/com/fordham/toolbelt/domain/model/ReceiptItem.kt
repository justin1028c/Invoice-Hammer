package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID

data class ReceiptItem(
    val id: ReceiptId = ReceiptId(randomUUID()),
    val description: String,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val category: String = "Other",
    val clientName: String = "",
    val imagePath: String = "",
    val isBilled: Boolean = false,
    val lastUpdated: Long = DateTimeUtil.nowEpochMillis(),
    val supplierName: String = "",
    val linkedInvoiceId: InvoiceId? = null
) {
    val formattedPrice: String get() = DateTimeUtil.formatMoney(totalPrice)
    val formattedDetails: String get() = "${if (quantity % 1.0 == 0.0) quantity.toInt() else quantity} @ ${DateTimeUtil.formatMoney(unitPrice)} | ${supplierName.ifEmpty { "General" }}"
}
