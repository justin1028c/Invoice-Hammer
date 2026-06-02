package com.fordham.toolbelt.domain.model.agent

/**
 * Builds a single quick-invoice line item from flat Gemini parameters.
 */
object ForemanQuickInvoiceLineItems {
    fun fromFlatFields(
        jobDescription: String = "",
        category: String = "",
        totalAmount: Double? = null
    ): List<DraftLineItemInput> {
        val amount = totalAmount ?: 0.0
        val description = jobDescription.trim().ifBlank { "Service" }
        val normalizedCategory = ForemanInvoiceCategory.normalize(category)
        if (description == "Service" && amount <= 0.0 && category.isBlank()) return emptyList()

        return listOf(
            DraftLineItemInput(
                description = NaturalLanguage(description),
                amount = amount,
                category = NaturalLanguage(normalizedCategory),
                quantity = if (amount > 0.0) 1.0 else null,
                unitPrice = if (amount > 0.0) amount else null
            )
        )
    }
}
