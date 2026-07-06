package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.ItemsSummary
import com.fordham.toolbelt.domain.model.LineItem
import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.util.AppLogger
import kotlin.math.roundToLong

internal sealed interface VoiceToken {
    val raw: String
    val range: IntRange

    data class Quantity(val value: Double, override val raw: String, override val range: IntRange) : VoiceToken
    data class Price(val cents: Long, val unit: PriceUnit, override val raw: String, override val range: IntRange) : VoiceToken
    data class Action(val type: ActionType, override val raw: String, override val range: IntRange) : VoiceToken
    data class Deposit(override val raw: String, override val range: IntRange) : VoiceToken
    data class Tax(override val raw: String, override val range: IntRange) : VoiceToken
    data class Word(override val raw: String, override val range: IntRange) : VoiceToken
}

internal enum class PriceUnit {
    Flat,
    Each,
    PerFoot,
    PerSquareFoot,
    PerHour
}

internal enum class ActionType {
    Install,
    Paint,
    Repair,
    Replace,
    Haul,
    Seal,
    PressureWash,
    Add,
    Generic
}

internal data class ParsedVoiceLine(
    val quantity: Double?,
    val description: String,
    val amountCents: Long,
    val unitPriceCents: Long?,
    val category: String,
    val confidence: Double,
    val ambiguityReason: String? = null
) {
    fun toLineItem(): LineItem = LineItem(
        description = ItemsSummary(description),
        amount = MoneyAmount(amountCents / 100.0),
        category = category,
        quantity = quantity,
        unitPrice = unitPriceCents?.let { MoneyAmount(it / 100.0) }
    )
}

internal sealed interface VoiceInvoiceParserOutcome {
    data class Success(val lines: List<ParsedVoiceLine>) : VoiceInvoiceParserOutcome
    data class Ambiguous(val lines: List<ParsedVoiceLine>, val reasons: List<String>) : VoiceInvoiceParserOutcome
    data class Failure(val reason: String) : VoiceInvoiceParserOutcome
}

internal class VoiceInvoiceTokenizer {
    private val tokenPattern = Regex("""\$?\d+(?:\.\d+)?|[A-Za-z']+|%""")
    private val numberWords = mapOf(
        "one" to 1.0,
        "won" to 1.0,
        "two" to 2.0,
        "too" to 2.0,
        "to" to 2.0,
        "three" to 3.0,
        "four" to 4.0,
        "for" to 4.0,
        "five" to 5.0,
        "six" to 6.0,
        "seven" to 7.0,
        "eight" to 8.0,
        "nine" to 9.0,
        "ten" to 10.0,
        "eleven" to 11.0,
        "twelve" to 12.0
    )

    fun tokenize(text: String): List<VoiceToken> {
        val matches = tokenPattern.findAll(text).toList()
        return matches.mapIndexed { index, match ->
            val raw = match.value
            val lower = raw.lowercase()
            val next = matches.getOrNull(index + 1)?.value?.lowercase()
            val next2 = matches.getOrNull(index + 2)?.value?.lowercase()
            val next3 = matches.getOrNull(index + 3)?.value?.lowercase()

            when {
                lower == "deposit" -> VoiceToken.Deposit(raw, match.range)
                lower == "tax" || raw == "%" -> VoiceToken.Tax(raw, match.range)
                actionTypeAt(matches, index) != null -> VoiceToken.Action(actionTypeAt(matches, index)!!, raw, match.range)
                raw.startsWith("$") || raw.firstOrNull()?.isDigit() == true -> {
                    val amount = raw.trimStart('$').toDoubleOrNull()
                    if (amount != null && isPriceContext(raw, next, next2, next3)) {
                        VoiceToken.Price(
                            cents = if (next == "cents" || next == "cent") (amount * 1.0).roundToLong() else (amount * 100.0).roundToLong(),
                            unit = priceUnit(next, next2, next3),
                            raw = raw,
                            range = match.range
                        )
                    } else {
                        VoiceToken.Quantity(amount ?: 0.0, raw, match.range)
                    }
                }
                numberWords[lower] != null -> VoiceToken.Quantity(numberWords.getValue(lower), raw, match.range)
                else -> VoiceToken.Word(raw, match.range)
            }
        }
    }

    private fun isPriceContext(raw: String, next: String?, next2: String?, next3: String?): Boolean {
        if (raw.startsWith("$")) return true
        return next in setOf("dollar", "dollars", "buck", "bucks", "cent", "cents", "each", "apiece") ||
            next == "per" ||
            (next == "an" && next2 == "hour") ||
            (next == "a" && next2 in setOf("piece", "hour")) ||
            next2 == "each" ||
            next3 == "each"
    }

    private fun priceUnit(next: String?, next2: String?, next3: String?): PriceUnit = when {
        next in setOf("each", "apiece") -> PriceUnit.Each
        next == "per" && next2 == "square" && next3 in setOf("foot", "feet", "ft") -> PriceUnit.PerSquareFoot
        next == "per" && next2 in setOf("foot", "feet", "ft", "linear") -> PriceUnit.PerFoot
        next == "per" && next2 in setOf("hour", "hr") -> PriceUnit.PerHour
        next == "an" && next2 == "hour" -> PriceUnit.PerHour
        next == "a" && next2 == "piece" -> PriceUnit.Each
        next2 == "each" || next3 == "each" -> PriceUnit.Each
        else -> PriceUnit.Flat
    }

    private fun actionTypeAt(matches: List<MatchResult>, index: Int): ActionType? {
        val lower = matches[index].value.lowercase()
        val next = matches.getOrNull(index + 1)?.value?.lowercase()
        return when {
            lower in setOf("installed", "install") -> ActionType.Install
            lower in setOf("painted", "paint") -> ActionType.Paint
            lower in setOf("repaired", "repair", "fixed", "fix") -> ActionType.Repair
            lower in setOf("replaced", "replace") -> ActionType.Replace
            lower in setOf("hauled", "haul", "removed", "remove") -> ActionType.Haul
            lower in setOf("sealed", "seal") -> ActionType.Seal
            lower in setOf("silver") -> ActionType.Seal
            lower == "pressure" && next in setOf("washed", "wash") -> ActionType.PressureWash
            lower == "washed" || (lower == "go" && next == "wash") || (lower == "to" && next in setOf("washed", "wash")) -> ActionType.PressureWash
            lower in setOf("add", "added", "apply") -> ActionType.Add
            else -> null
        }
    }
}

internal object ServiceRegistry {
    private val aliases = mapOf(
        "go wash" to "Pressure washed",
        "to washed" to "Pressure washed",
        "to wash" to "Pressure washed",
        "back the steps" to "Sealed back steps",
        "silver front steps" to "Sealed front steps",
        "silver back steps" to "Sealed back steps",
        "away the old unit" to "Hauled away the old unit",
        "away old unit" to "Hauled away the old unit",
        "material pickup fee" to "Material pickup fee",
        "markup fee" to "Markup fee",
        "hallway trim" to "Hallway trim",
        "garbage disposal" to "Garbage disposal",
        "gsci" to "GFCI",
        "tsci" to "GFCI",
        "shut off valve" to "shut off valve",
        "shut off valves" to "shut off valves",
        "nail pops" to "nail pops"
    )

    fun cleanDescription(raw: String): String {
        var text = raw
            .replace(Regex("""(?i)\bwe\s+"""), "")
            .replace(Regex("""(?i)\bput mud on it\b"""), "mudded")
            .replace(Regex("""(?i)\bsilver\b"""), "sealed")
            .replace(Regex("""(?i)\b(?:gsci|tsci)\b"""), "GFCI")
            .replace(Regex("""(?i)^around\s+(?=\d+(?:\.\d+)?\s+(?:linear\s+feet|feet|ft)\s+of\s+wire\b)"""), "ran ")
            .replace(Regex("""(?i)\bstart\s+(\d+(?:\.\d+)?)\s+dedicated\s+la\b"""), "installed $1 dedicated outlet")
            .replace(Regex("""(?i)\btaped it\b"""), "taped")
            .replace(Regex("""(?i)\bskimmed it\b"""), "skimmed")
            .replace(Regex("""(?i)\breplace\b"""), "replaced")
            .replace(Regex("""(?i)^(?:a|an|the)\s+"""), "")
            .replace(Regex("""(?i)^(?:foot|feet|ft|square\s+foot|square\s+feet)\s+"""), "")
            .replace(Regex("""(?i)\b(?:to|for|at|and|is)$"""), "")
            .trim(' ', ',', '.', '-')
            .replace(Regex("""\s+"""), " ")

        val lower = text.lowercase()
        aliases.entries.firstOrNull { (alias, _) -> lower.contains(alias) }?.let { (alias, canonical) ->
            text = if (alias in setOf("shut off valve", "shut off valves", "nail pops")) {
                text.replace(Regex(Regex.escape(alias), RegexOption.IGNORE_CASE), canonical)
            } else {
                canonical + text.substringAfter(alias, "").trimStart().let { if (it.isBlank()) "" else " $it" }
            }
        }
        return text.replaceFirstChar { it.uppercase() }
    }
}

internal class VoiceInvoiceClauseParser {
    private val tokenizer = VoiceInvoiceTokenizer()
    private val priceTailPattern = Regex(
        """(?i)\s*(?:at|for|is)?\s*\$?\d+(?:\.\d+)?\s*(?:dollars?|bucks?|cents?)?(?:\s+(?:each|apiece|per\s+(?:square\s+foot|square\s+feet|linear\s+foot|linear\s+feet|foot|feet|ft|hour|hr)|an?\s+hour|a\s+piece))?\s*"""
    )

    fun parse(text: String): VoiceInvoiceParserOutcome {
        val clauses = splitClauses(text)
        if (clauses.isEmpty()) return VoiceInvoiceParserOutcome.Failure("NO_PRICE_CLAUSES")

        val lines = clauses.mapNotNull(::parseClause)
        if (lines.isEmpty()) return VoiceInvoiceParserOutcome.Failure("NO_LINE_ITEMS")

        val ambiguous = lines.mapNotNull { it.ambiguityReason }
        AppLogger.d("VoiceClauseParser", "clauses=${clauses.joinToString(" | ")} lines=${lines.joinToString { "${it.description}:${it.amountCents}" }}")
        return if (ambiguous.isEmpty()) {
            VoiceInvoiceParserOutcome.Success(lines)
        } else {
            VoiceInvoiceParserOutcome.Ambiguous(lines, ambiguous)
        }
    }

    private fun splitClauses(text: String): List<String> {
        val withoutDeposit = text.replace(
            Regex("""(?i)\b(?:and\s+)?(?:apply|add)?\s*(?:at\s+)?\$?\d+(?:\.\d+)?\s*(?:dollars?|bucks?)?\s+deposit\b.*$"""),
            ""
        )
        val actionBoundary = Regex(
            """(?i)\s+(?=(?:we\s+)?(?:installed?|painted?|repaired?|fixed|replaced?|hauled|removed|sealed|silver|pressure\s+washed|go\s+wash|to\s+washed?|back\s+the\s+steps|away\s+(?:the\s+)?old\s+unit|add(?:ed)?|apply|did)\b)"""
        )
        return actionBoundary.split(withoutDeposit)
            .map { it.trim(' ', ',', '.', '-') }
            .filter { clause ->
                clause.isNotBlank() &&
                    Regex("""(?i)(?:\$|\b\d+(?:\.\d+)?\s*(?:dollars?|bucks?|cents?|each|per\b)|\bfor\s+\d+(?:\.\d+)?)""").containsMatchIn(clause)
            }
    }

    private fun parseClause(clause: String): ParsedVoiceLine? {
        val tokens = tokenizer.tokenize(clause)
        val price = tokens.filterIsInstance<VoiceToken.Price>().firstOrNull() ?: return null
        if (tokens.any { it is VoiceToken.Deposit }) return null
        val actionType = tokens.filterIsInstance<VoiceToken.Action>().firstOrNull()?.type

        val rawBeforePrice = clause.substring(0, price.range.first.coerceAtMost(clause.length)).trim()
        val phrase = rawBeforePrice
            .substringAfterLast(" invoice for ", rawBeforePrice)
            .substringAfterLast(" at ", rawBeforePrice)
            .replace(Regex("""(?i)^.*?\b(?:installed?|painted?|repaired?|fixed|replaced?|hauled|removed|sealed|silver|pressure\s+washed?|pressure\s+wash|did|add(?:ed)?|apply)\s+"""), "")
            .replace(Regex("""(?i)^to\s+(?=(?:nail\s+pops?|shut\s+off\s+valves?|outlets?|receptacles?|valves?|boards?|fixtures?)\b)"""), "2 ")
            .trim()

        val quantity = extractQuantity(phrase, price.unit)
        val unitPriced = price.unit != PriceUnit.Flat || Regex("""(?i)\b(each|apiece|per)\b""").containsMatchIn(clause)
        val amountCents = if (unitPriced && quantity != null) (price.cents * quantity).roundToLong() else price.cents
        val unitPriceCents = if (unitPriced) price.cents else null
        val description = ServiceRegistry.cleanDescription(phrase)
            .withActionPrefix(actionType)
            .let { cleaned ->
                if (actionType == ActionType.Seal && !cleaned.startsWith("Sealed ", ignoreCase = true)) {
                    "Sealed ${cleaned.replaceFirstChar { it.lowercase() }}"
                } else {
                    cleaned
                }
            }
            .ifBlank { return null }
        val ambiguity = when {
            unitPriced && quantity == null -> "UNIT_PRICE_WITHOUT_QUANTITY:$clause"
            description.length <= 2 -> "INCOHERENT_DESCRIPTION:$clause"
            Regex("""(?i)^(?:to|for|at)\b""").containsMatchIn(description) -> "INCOHERENT_DESCRIPTION:$clause"
            Regex("""(?i)\b(?:to|for|at|is)$""").containsMatchIn(description) -> "INCOHERENT_DESCRIPTION:$clause"
            else -> null
        }

        return ParsedVoiceLine(
            quantity = quantity ?: 1.0,
            description = description,
            amountCents = amountCents,
            unitPriceCents = unitPriceCents,
            category = inferCategory(description),
            confidence = if (ambiguity == null) 0.96 else 0.55,
            ambiguityReason = ambiguity
        )
    }

    private fun extractQuantity(phrase: String, unit: PriceUnit): Double? {
        val repaired = phrase.replace(
            Regex("""(?i)^to\s+(?=(?:nail\s+pops?|shut\s+off\s+valves?|outlets?|receptacles?|valves?|boards?|fixtures?)\b)"""),
            "2 "
        )
        Regex("""(?i)^\s*(\d+(?:\.\d+)?)\b""").find(repaired)?.let {
            return it.groupValues[1].toDoubleOrNull()
        }
        Regex("""(?i)\b(\d+(?:\.\d+)?)\s+(?:linear\s+feet|square\s+feet|feet|ft|shut|nail|light|outlet|receptacle|valve|board|boards)\b""")
            .find(repaired)
            ?.let { return it.groupValues[1].toDoubleOrNull() }
        return if (unit == PriceUnit.Flat) null else null
    }

    private fun inferCategory(description: String): String {
        val lower = description.lowercase()
        return when {
            listOf("hour", "labor", "labour", "hora").any(lower::contains) -> "Labor"
            listOf("drywall", "sheetrock", "tablaroca", "mud", "skim", "tape").any(lower::contains) -> "Drywall"
            listOf("wire", "circuit", "breaker", "light fixture", "fixture", "receptacle", "outlet", "gfci", "electrical").any(lower::contains) -> "Electrical"
            listOf("toilet", "sink", "faucet", "plumbing", "disposal", "valve").any(lower::contains) -> "Plumbing"
            listOf(
                "deck", "board", "boards", "stair", "tread", "handrail", "rail", "lumber",
                "framed", "frame", "closet", "prehung", "door", "baseboard", "shelving",
                "shelf", "window sill", "sill", "casing", "carpentry"
            ).any(lower::contains) -> "Carpentry"
            listOf("paint", "painting", "painted").any(lower::contains) -> "Painting"
            listOf("carpet", "floor", "flooring").any(lower::contains) -> "Flooring"
            listOf("siding", "general repair", "repair").any(lower::contains) -> "General Repair"
            listOf("material", "materials", "markup").any(lower::contains) -> "Materials"
            else -> "Service"
        }
    }

    private fun String.withActionPrefix(actionType: ActionType?): String {
        if (hasActionPrefix()) return this
        val prefix = when (actionType) {
            ActionType.Install -> "Installed"
            ActionType.Paint -> "Painted"
            ActionType.Repair -> "Repaired"
            ActionType.Replace -> "Replaced"
            ActionType.Haul -> "Hauled"
            ActionType.PressureWash -> "Pressure washed"
            else -> null
        } ?: return this
        return if (startsWith(prefix, ignoreCase = true)) {
            this
        } else {
            "$prefix ${replaceFirstChar { it.lowercase() }}"
        }
    }

    private fun String.hasActionPrefix(): Boolean =
        Regex("""(?i)^(?:installed|painted|repaired|replaced|hauled|removed|sealed|pressure\s+washed)\b""")
            .containsMatchIn(this)
}
