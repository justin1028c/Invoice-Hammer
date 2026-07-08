package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.AiInvoiceResult
import com.fordham.toolbelt.domain.model.ItemsSummary
import com.fordham.toolbelt.domain.model.LineItem

class NormalizeVoiceInvoiceLineItemsUseCase {
    operator fun invoke(input: AiInvoiceResult): AiInvoiceResult {
        if (input.items.isEmpty()) return input
        return input.copy(items = input.items.map(::normalizeLineItem))
    }

    private fun normalizeLineItem(item: LineItem): LineItem {
        val normalized = normalizeDescription(item.description.value, item.category)
        return if (normalized == item.description.value) {
            item
        } else {
            item.copy(description = ItemsSummary(normalized))
        }
    }

    private fun normalizeDescription(description: String, category: String): String {
        val trimmed = description.trim()
        val lower = trimmed.lowercase()
        val categoryLower = category.trim().lowercase()

        val directExpansion = ShortDescriptionExpansions[lower]
        if (directExpansion != null) return directExpansion

        val categoryExpansion = ShortDescriptionExpansions[categoryLower]
            ?.takeIf { trimmed.length < MIN_CLIENT_FACING_DESCRIPTION_LENGTH }
            ?: return trimmed

        return categoryExpansion
    }

    companion object {
        private const val MIN_CLIENT_FACING_DESCRIPTION_LENGTH = 8
        private val ShortDescriptionExpansions = mapOf(
            "labor" to "Labor service",
            "labour" to "Labor service",
            "mano de obra" to "Labor service",
            "service" to "General service",
            "servicio" to "General service",
            "materials" to "Materials and supplies",
            "material" to "Materials and supplies",
            "materiales" to "Materials and supplies",
            "paint" to "Painting work",
            "painting" to "Painting work",
            "pintura" to "Painting work",
            "drywall" to "Drywall repair",
            "sheetrock" to "Drywall repair",
            "carpentry" to "Carpentry work",
            "carpinteria" to "Carpentry work",
            "carpintería" to "Carpentry work",
            "flooring" to "Flooring work",
            "plumbing" to "Plumbing work",
            "electrical" to "Electrical work",
            "roofing" to "Roofing work"
        )
    }
}
