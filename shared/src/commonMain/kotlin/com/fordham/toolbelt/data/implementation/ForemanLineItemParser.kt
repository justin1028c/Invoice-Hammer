package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.agent.DraftLineItemInput
import com.fordham.toolbelt.domain.model.agent.ForemanInvoiceCategory
import com.fordham.toolbelt.domain.model.agent.ForemanQuickInvoiceLineItems
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.util.AiUtil
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

internal object ForemanLineItemParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun resolveQuickInvoiceLineItems(
        lineItemsJson: String,
        jobDescription: String = "",
        category: String = "",
        totalAmount: Double? = null
    ): List<DraftLineItemInput> {
        val parsed = parse(lineItemsJson)?.map { item ->
            item.copy(category = NaturalLanguage(ForemanInvoiceCategory.normalize(item.category.value)))
        }
        if (!parsed.isNullOrEmpty()) return parsed
        return ForemanQuickInvoiceLineItems.fromFlatFields(
            jobDescription = jobDescription,
            category = category,
            totalAmount = totalAmount
        )
    }

    fun parse(raw: String): List<DraftLineItemInput>? {
        if (raw.isBlank() || raw == "[]") return emptyList()
        return try {
            val cleaned = AiUtil.cleanJson(raw)
            val element = json.parseToJsonElement(cleaned)
            val array = when (element) {
                is JsonArray -> element
                is JsonObject -> {
                    val itemsKey = element.keys.firstOrNull {
                        it.equals("items", ignoreCase = true) ||
                            it.equals("lineItems", ignoreCase = true)
                    }
                    if (itemsKey != null) element[itemsKey] as? JsonArray else JsonArray(listOf(element))
                }
                else -> null
            } ?: return null

            array.map { item ->
                val obj = item as? JsonObject ?: return null
                val descKey = obj.keys.firstOrNull {
                    it.equals("description", ignoreCase = true) ||
                        it.equals("name", ignoreCase = true) ||
                        it.equals("title", ignoreCase = true)
                }
                val description = descKey?.let { obj[it]?.jsonPrimitive?.content } ?: ""
                val amountKey = obj.keys.firstOrNull {
                    it.equals("amount", ignoreCase = true) ||
                        it.equals("price", ignoreCase = true) ||
                        it.equals("cost", ignoreCase = true) ||
                        it.equals("total", ignoreCase = true)
                }
                var amount = amountKey?.let { obj[it]?.jsonPrimitive?.doubleOrNull } ?: 0.0
                
                val qtyKey = obj.keys.firstOrNull {
                    it.equals("quantity", ignoreCase = true) ||
                        it.equals("qty", ignoreCase = true) ||
                        it.equals("hours", ignoreCase = true) ||
                        it.equals("hrs", ignoreCase = true) ||
                        it.equals("units", ignoreCase = true)
                }
                val quantity = qtyKey?.let { obj[it]?.jsonPrimitive?.doubleOrNull }
                
                val priceKey = obj.keys.firstOrNull {
                    it.equals("unitPrice", ignoreCase = true) ||
                        it.equals("unit_price", ignoreCase = true) ||
                        it.equals("rate", ignoreCase = true) ||
                        it.equals("pricePerUnit", ignoreCase = true) ||
                        it.equals("price_per_unit", ignoreCase = true)
                }
                val unitPrice = priceKey?.let { obj[it]?.jsonPrimitive?.doubleOrNull }
                
                if (amount == 0.0 && quantity != null && unitPrice != null) {
                    amount = quantity * unitPrice
                }
                
                val catKey = obj.keys.firstOrNull {
                    it.equals("category", ignoreCase = true) || it.equals("type", ignoreCase = true)
                }
                val category = ForemanInvoiceCategory.normalize(
                    catKey?.let { obj[it]?.jsonPrimitive?.content }.orEmpty().ifBlank { "Service" }
                )
                DraftLineItemInput(
                    description = NaturalLanguage(description),
                    amount = amount,
                    category = NaturalLanguage(category),
                    quantity = quantity,
                    unitPrice = unitPrice
                )
            }
        } catch (_: Throwable) {
            null
        }
    }
}
