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
        description = com.fordham.toolbelt.domain.model.ItemsSummary(description),
        amount = com.fordham.toolbelt.domain.model.MoneyAmount(amount),
        category = category,
        quantity = quantity,
        unitPrice = unitPrice?.let { com.fordham.toolbelt.domain.model.MoneyAmount(it) }
    )

    companion object {
        fun fromDomain(domain: LineItem) = LineItemDto(
            description = domain.description.value,
            amount = domain.amount.value,
            category = domain.category,
            quantity = domain.quantity,
            unitPrice = domain.unitPrice?.value
        )
    }
}

@Serializable
data class AiInvoiceResultDto(
    val clientName: String = "",
    val clientAddress: String = "",
    val items: List<LineItemDto> = emptyList(),
    val laborHours: Double? = null,
    val laborRate: Double? = null,
    val depositAmount: Double = 0.0,
    val taxRatePercent: Double = 7.0,
    val discountPercent: Double = 0.0,
    val notes: String = "",
    val confidenceScore: Double = 1.0,
    val userSummary: String = "",
    val validationIssues: List<String> = emptyList()
) {
    fun toDomain() = AiInvoiceResult(
        clientName = clientName,
        clientAddress = clientAddress,
        items = items.map { it.toDomain() },
        laborHours = laborHours,
        laborRate = laborRate,
        depositAmount = depositAmount,
        taxRatePercent = taxRatePercent,
        discountPercent = discountPercent,
        notes = notes,
        confidenceScore = confidenceScore,
        userSummary = userSummary,
        validationIssues = validationIssues
    )
}
