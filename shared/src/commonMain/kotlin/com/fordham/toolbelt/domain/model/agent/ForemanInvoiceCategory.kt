package com.fordham.toolbelt.domain.model.agent

/**
 * Invoice trade categories shown in the New Invoice tab.
 */
object ForemanInvoiceCategory {
    val KNOWN: List<String> = listOf(
        "Drywall",
        "Flooring",
        "Roofing",
        "Plumbing",
        "Electrical",
        "Painting",
        "Carpentry",
        "General Repair"
    )

    private val ALIASES: Map<String, String> = mapOf(
        "drywall" to "Drywall",
        "dry wall" to "Drywall",
        "sheetrock" to "Drywall",
        "sheet rock" to "Drywall",
        "flooring" to "Flooring",
        "floor" to "Flooring",
        "roofing" to "Roofing",
        "roof" to "Roofing",
        "plumbing" to "Plumbing",
        "plumber" to "Plumbing",
        "electrical" to "Electrical",
        "electric" to "Electrical",
        "electrician" to "Electrical",
        "painting" to "Painting",
        "paint" to "Painting",
        "carpentry" to "Carpentry",
        "carpenter" to "Carpentry",
        "general repair" to "General Repair",
        "general" to "General Repair",
        "repair" to "General Repair",
        "labor" to "General Repair",
        "service" to "General Repair",
        "materials" to "General Repair"
    )

    fun normalize(raw: String): String {
        val cleaned = raw.trim().replace(Regex("""\s+"""), " ")
        if (cleaned.isBlank()) return "General Repair"
        KNOWN.firstOrNull { it.equals(cleaned, ignoreCase = true) }?.let { return it }
        ALIASES[cleaned.lowercase()]?.let { return it }
        return cleaned.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { ch -> ch.uppercaseChar() }
        }
    }

    fun normalizeOrInfer(raw: String, description: String): String {
        val normalized = normalize(raw)
        if (normalized != "General Repair" || raw.isBlank()) {
            return inferFromDescription(description) ?: normalized
        }
        return inferFromDescription(description) ?: normalized
    }

    fun inferFromDescription(description: String): String? {
        val lower = description.lowercase()
        return when {
            Regex("""\b(?:drywall|sheetrock|sheet rock|mud(?:ded)?|tape(?:d)?|skim(?:med)?|patch(?:ed)? wall)\b""").containsMatchIn(lower) -> "Drywall"
            Regex("""\b(?:toilet|sink|faucet|drain|pipe|plumb|valve|garbage disposal|water heater|shut off)\b""").containsMatchIn(lower) -> "Plumbing"
            Regex("""\b(?:outlet|receptacle|light|fixture|fan|breaker|panel|wire|circuit|gfci|switch)\b""").containsMatchIn(lower) -> "Electrical"
            Regex("""\b(?:paint|painted|painting|stain|caulk|primer|trim)\b""").containsMatchIn(lower) -> "Painting"
            Regex("""\b(?:deck|board|handrail|baseboard|stair|tread|door|framing|wood|fence|cabinet)\b""").containsMatchIn(lower) -> "Carpentry"
            Regex("""\b(?:floor|flooring|tile|carpet|laminate|vinyl plank|lvp)\b""").containsMatchIn(lower) -> "Flooring"
            Regex("""\b(?:roof|shingle|gutter|flashing)\b""").containsMatchIn(lower) -> "Roofing"
            else -> null
        }
    }
}
