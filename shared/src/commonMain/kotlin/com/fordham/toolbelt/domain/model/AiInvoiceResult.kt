package com.fordham.toolbelt.domain.model

data class AiInvoiceResult(
    val clientName: String = "",
    val clientAddress: String = "",
    val items: List<LineItem> = emptyList()
)
