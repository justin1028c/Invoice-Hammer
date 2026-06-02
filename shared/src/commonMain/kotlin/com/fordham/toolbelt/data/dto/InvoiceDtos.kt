package com.fordham.toolbelt.data.dto

import com.fordham.toolbelt.domain.model.AiInvoiceResult
import com.fordham.toolbelt.domain.model.LineItem
import kotlinx.serialization.Serializable

@Serializable
data class LineItemDto(
    val description: String,
    val amount: Double,
    val category: String = "Service",
    val quantity: Double? = null,
    val unitPrice: Double? = null
) {
    fun toDomain() = LineItem(
        description = description,
        amount = amount,
        category = category,
        quantity = quantity,
        unitPrice = unitPrice
    )

    companion object {
        fun fromDomain(domain: LineItem) = LineItemDto(
            description = domain.description,
            amount = domain.amount,
            category = domain.category,
            quantity = domain.quantity,
            unitPrice = domain.unitPrice
        )
    }
}

@Serializable
data class AiInvoiceResultDto(
    val clientName: String = "",
    val clientAddress: String = "",
    val items: List<LineItemDto> = emptyList()
) {
    fun toDomain() = AiInvoiceResult(
        clientName = clientName,
        clientAddress = clientAddress,
        items = items.map { it.toDomain() }
    )
}
