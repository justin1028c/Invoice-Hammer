package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.LineItem
import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.domain.model.ClientName

enum class OpportunityConfidence {
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}

data class RecommendedAction(
    val title: NaturalLanguage,
    val reason: NaturalLanguage,
    val estimatedImpact: MoneyAmount
)

@kotlin.jvm.JvmInline
value class MoneyVariance(val value: Double)

data class ProfitGuardianStatus(
    val invoiceId: InvoiceId,
    val clientName: ClientName,
    val budgetedRevenue: MoneyAmount,
    val projectedRevenue: MoneyAmount,
    val budgetedMaterials: MoneyAmount,
    val actualMaterials: MoneyAmount,
    val materialVariance: MoneyVariance,
    val projectedProfit: MoneyAmount,
    val currentProjection: MoneyAmount,
    val reasons: List<NaturalLanguage>,
    val recommendations: List<NaturalLanguage>
) {
    val isTrendingNegative: Boolean get() = currentProjection.value < projectedProfit.value
}

data class ChangeOrderOpportunity(
    val invoiceId: InvoiceId,
    val clientName: ClientName,
    val detectedTask: NaturalLanguage,
    val recommendedItems: List<LineItem>,
    val estimatedValueRange: ClosedRange<Double>,
    val confidence: OpportunityConfidence
)

data class DailyBriefing(
    val timestamp: Long,
    val overdueInvoiceCount: Int,
    val totalOverdueAmount: MoneyAmount,
    val budgetOverruns: List<ProfitGuardianStatus>,
    val unbilledOpportunities: List<ChangeOrderOpportunity>,
    val potentialProfitRecovery: MoneyAmount,
    val primaryAction: RecommendedAction?
)

data class ParsedSystemBudget(
    val revenue: Double,
    val materials: Double,
    val lineItems: List<LineItem>
)

object SystemBudgetSerializer {
    private const val PREFIX = "[SYSTEM_BUDGET]"

    fun serialize(revenue: Double, materials: Double, lineItems: List<LineItem>): String {
        val itemsStr = lineItems.joinToString("|") { item ->
            val desc = item.description.value.replace(":", "\\:").replace("|", "\\|")
            val category = item.category.replace(":", "\\:").replace("|", "\\|")
            "$desc:$category:${item.amount.value}:${item.quantity ?: ""}:${item.unitPrice?.value ?: ""}"
        }
        return "$PREFIX revenue=$revenue; materials=$materials; items=$itemsStr"
    }

    fun deserialize(text: String): ParsedSystemBudget? {
        if (!text.startsWith(PREFIX)) return null
        val content = text.removePrefix(PREFIX).trim()
        val parts = content.split(";")
        var revenue = 0.0
        var materials = 0.0
        var lineItems = emptyList<LineItem>()
        for (part in parts) {
            val kv = part.split("=")
            if (kv.size != 2) continue
            val key = kv[0].trim()
            val value = kv[1].trim()
            when (key) {
                "revenue" -> revenue = value.toDoubleOrNull() ?: 0.0
                "materials" -> materials = value.toDoubleOrNull() ?: 0.0
                "items" -> {
                    if (value.isNotEmpty()) {
                        val itemParts = value.split("|")
                        lineItems = itemParts.mapNotNull { itemStr ->
                            val fields = itemStr.split(":")
                            if (fields.size < 3) return@mapNotNull null
                            val desc = fields[0].replace("\\:", ":").replace("\\|", "|")
                            val category = fields[1].replace("\\:", ":").replace("\\|", "|")
                            val amount = fields[2].toDoubleOrNull() ?: 0.0
                            val qty = fields.getOrNull(3)?.toDoubleOrNull()
                            val unitPrice = fields.getOrNull(4)?.toDoubleOrNull()
                            LineItem(
                                description = com.fordham.toolbelt.domain.model.ItemsSummary(desc),
                                amount = com.fordham.toolbelt.domain.model.MoneyAmount(amount),
                                category = category,
                                quantity = qty,
                                unitPrice = unitPrice?.let { com.fordham.toolbelt.domain.model.MoneyAmount(it) }
                            )
                        }
                    }
                }
            }
        }
        return ParsedSystemBudget(revenue, materials, lineItems)
    }
}
