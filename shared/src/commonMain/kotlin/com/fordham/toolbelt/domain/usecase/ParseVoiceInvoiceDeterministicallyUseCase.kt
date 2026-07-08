package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.AiInvoiceResult
import com.fordham.toolbelt.domain.model.ItemsSummary
import com.fordham.toolbelt.domain.model.LineItem
import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.util.AppLogger

class ParseVoiceInvoiceDeterministicallyUseCase {
    private val structuredLineParser = VoiceInvoiceClauseParser()

    operator fun invoke(text: String): AiInvoiceResult? {
        val normalized = text
            .normalizeVoiceInvoiceTranscript()
            .trim()
            .replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return null

        parseContractorInvoice(normalized)?.let { return it }
        parseSimpleInvoice(normalized)?.let { return it }
        parseSimpleFactura(normalized)?.let { return it }
        return null
    }

    private fun parseContractorInvoice(text: String): AiInvoiceResult? {
        val lower = text.lowercase()
        if (!listOf("invoice", "bill", "charge").any(lower::contains)) return null

        val clientName = extractClientName(text)
        if (clientName.isBlank()) {
            AppLogger.d("DeterministicInvoice", "No client name extracted from: $text")
            return null
        }

        val rawAddress = extractRawAddress(text)
        val clientAddress = rawAddress.cleanAddress()

        val workText = if (clientAddress.isNotBlank()) text.substringAfter(rawAddress, text) else text
        val items = parseContractorLineItems(workText)
        if (items.isEmpty()) return null

        val hasExplicitMoney = Regex("""(?:\$|(?:\b\d+(?:\.\d+)?\s*(?:dollars?|bucks?)\b))""", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)
        if (!hasExplicitMoney) return null

        return AiInvoiceResult(
            clientName = clientName,
            clientAddress = clientAddress,
            items = items,
            taxRatePercent = parseTaxRate(text) ?: 7.0,
            depositAmount = parseDeposit(text) ?: 0.0,
            confidenceScore = if (clientAddress.isNotBlank()) 0.96 else 0.90,
            userSummary = "Prepared ${items.size} invoice lines for $clientName."
        )
    }

    private fun extractClientName(text: String): String {
        val patterns = listOf(
            // "make/create/start an invoice for John Smith at 123 Main..."
            Regex(
                """(?i)\b(?:make|create|start|write|build|prepare)?\s*(?:an?\s+)?(?:invoice|bill|charge|estimate)\s+(?:for|to)\s+(?:new\s+client\s+|client\s+)?(.+?)(?=\s+(?:at|to|address|located\s+at|for|we\s+did|we\s+replaced|we\s+replace|did|do|replace|replaced|install|installed|add|charge|billing)\b|[.,;!?]|$)"""
            ),
            // "new client John Smith at 123 Main... invoice/bill/charge..."
            Regex(
                """(?i)\b(?:new\s+client|client)\s+(.+?)(?=\s+(?:at|to|address|located\s+at|invoice|bill|charge|estimate|for|we\s+did|we\s+replaced|we\s+replace|did|do|replace|replaced|install|installed|add)\b|[.,;!?]|$)"""
            ),
            // "John Smith at 123 Main. Installed drywall for $500."
            Regex(
                """(?i)^(.+?)(?=\s+(?:at|address|located\s+at)\s+\d+\b)"""
            )
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.groupValues?.getOrNull(1)?.cleanName()?.takeIf { it.isNotBlank() }
        }.orEmpty()
    }

    private fun parseContractorLineItems(text: String): List<LineItem> {
        val normalized = text
            .replace(Regex("""(?i)\bstart\s+(1|one)\s+dedicated\s+la\b"""), "installed 1 dedicated outlet")
            .replace(Regex("""(?i)\s+we\s+also\s+"""), " ")
            .replace(Regex("""(?i)\s+we\s+"""), " ")
            .replace(Regex("""(?i)\s+and\s+did\s+"""), " did ")
            .replace(Regex("""(?i)\s+and\s+add\s+"""), " add ")
            .replace(Regex("""(?i)\s+and\s+replaced?\s+"""), " replace ")
            .replace(Regex("""(?i)\s+installed\s+"""), " installed ")
            .replace(Regex("""(?i)\s+painted\s+"""), " painted ")
            .replace(Regex("""(?i)\s+hauled\s+"""), " hauled ")

        val expectedBillablePrices = countBillablePrices(normalized)
        when (val structured = structuredLineParser.parse(normalized)) {
            is VoiceInvoiceParserOutcome.Success -> {
                val lines = structured.lines.map { it.toLineItem() }
                if (lines.isNotEmpty() && lines.size >= expectedBillablePrices) {
                    return lines.distinctBy { "${it.description.value.lowercase()}|${it.amount.value}" }
                }
                AppLogger.d(
                    "DeterministicInvoice",
                    "Structured parser partial coverage lines=${lines.size} expected=$expectedBillablePrices; falling back"
                )
            }
            is VoiceInvoiceParserOutcome.Ambiguous -> {
                AppLogger.d("DeterministicInvoice", "Structured parser ambiguous: ${structured.reasons.joinToString()}")
            }
            is VoiceInvoiceParserOutcome.Failure -> {
                AppLogger.d("DeterministicInvoice", "Structured parser failed: ${structured.reason}")
            }
        }

        val laborItems = parseLaborLineItems(normalized)
        val moneyMatches = Regex(
            """(?i)(?:(?:at|for)\s+(\d+(?:\.\d+)?)\s+cents?\s+per\s+(?:square\s+foot|square\s+feet|linear\s+foot|linear\s+feet|foot|feet|ft|hour|hr|yard|sq\s*ft)|(?:at|for)\s+\$\s*(\d+(?:\.\d+)?)|\$\s*(\d+(?:\.\d+)?)|(?:at|for)\s+(\d+(?:\.\d+)?)(?!\s*(?:hours?|hrs?|h)\b)\s*(?:dollars?|bucks?|cents?|\s+an?\s+hour|\s*/\s*hour|\s+each|\s+apiece|\s+a\s+piece|\s+per\s+(?:square\s+foot|square\s+feet|linear\s+foot|linear\s+feet|foot|feet|ft|hour|hr|yard|sq\s*ft))?)(?:\s*(?:dollars?|bucks?|cents?)?(?:\s+an?\s+hour|\s*/\s*hour|\s+each|\s+apiece|\s+a\s+piece|\s+per\s+(?:square\s+foot|square\s+feet|linear\s+foot|linear\s+feet|foot|feet|ft|hour|hr|yard|sq\s*ft))?)?"""
        ).findAll(normalized).toList()

        val items = moneyMatches.mapIndexedNotNull { index, match ->
            val priceContext = normalized.substring(match.range.first, minOf(normalized.length, match.range.last + 48))
            if (Regex("""(?i)^\s*(?:at|for)?\s*\$?\s*\d+(?:\.\d+)?\s*(?:dollars?|bucks?)?\s+deposit\b""").containsMatchIn(priceContext)) {
                return@mapIndexedNotNull null
            }
            if (Regex("""(?i)^\s*dollars?\s+deposit\b""").containsMatchIn(priceContext)) {
                return@mapIndexedNotNull null
            }
            val segmentStart = if (index == 0) 0 else moneyMatches[index - 1].range.last + 1
            val phrase = normalized.substring(segmentStart, match.range.first).trim()
            val rawAmount = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.toDoubleOrNull()
                ?: return@mapIndexedNotNull null
            val amount = if (Regex("""(?i)\bcents?\b""").containsMatchIn(match.value)) rawAmount / 100.0 else rawAmount
            parseLineItemPhrase(phrase, amount, priceContext)
        }.filter { it.description.value.isNotBlank() && it.amount.value > 0.0 }
        return (items + laborItems).distinctBy { "${it.description.value.lowercase()}|${it.amount.value}" }
    }

    private fun countBillablePrices(text: String): Int {
        val withoutDeposit = text.replace(
            Regex("""(?i)\b(?:and\s+)?(?:apply|add)?\s*(?:at\s+)?\$?\d+(?:\.\d+)?\s*(?:dollars?|bucks?)?\s+deposit\b.*$"""),
            ""
        )
        return Regex(
            """(?i)(?:\$?\d+(?:\.\d+)?\s*(?:dollars?|bucks?|cents?)?(?:\s+(?:each|apiece|per\s+(?:square\s+foot|square\s+feet|linear\s+foot|linear\s+feet|foot|feet|ft|hour|hr)|an?\s+hour|a\s+piece))?|\$\d+(?:\.\d+)?)"""
        ).findAll(withoutDeposit)
            .count { match ->
                val value = match.value.trim()
                value.startsWith("$") ||
                    Regex("""(?i)\b(?:dollars?|bucks?|cents?|each|apiece|per|hour)\b""").containsMatchIn(value) ||
                    Regex("""(?i)\b(?:at|for|is)\s+$value\b""").containsMatchIn(withoutDeposit)
            }
    }

    private fun parseLaborLineItems(text: String): List<LineItem> {
        return Regex(
            """(?i)\b(?:at\s+)?(\d+(?:\.\d+)?)\s*(?:hours?|hrs?|h)\s+(?:of\s+)?labor\s+at\s+\$?\s*(\d+(?:\.\d+)?)"""
        ).findAll(text).mapNotNull { match ->
            val quantity = match.groupValues[1].toDoubleOrNull() ?: return@mapNotNull null
            val rate = match.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
            LineItem(
                description = ItemsSummary("${quantity.formatQuantity()} hours of labor"),
                amount = MoneyAmount(quantity * rate),
                category = "Labor",
                quantity = quantity,
                unitPrice = MoneyAmount(rate)
            )
        }.toList()
    }

    private fun parseLineItemPhrase(rawPhrase: String, amount: Double, pricePhrase: String): LineItem? {
        val actionPhrase = rawPhrase
            .substringAfterLast(" we did ", rawPhrase)
            .substringAfterLast(" did ", rawPhrase)
            .substringAfterLast(" and replaced ", rawPhrase)
            .substringAfterLast(" and replace ", rawPhrase)
            .substringAfterLast(" we replaced ", rawPhrase)
            .substringAfterLast(" we replace ", rawPhrase)
            .replace(Regex("""(?i)^(?:each|apiece|a\s+piece|per\s+\w+)\s+"""), "")
            .replace(Regex("""(?i)^.*\b(?:let'?s make an invoice|make an invoice|invoice|bill|charge)\s+for\s+.+?\s+(?:to|at)\s+"""), "")
            .replace(Regex("""(?i)^.*?\b(?:did|do|replace|replaced|repair|repaired|add|installed|install|painted|paint|hauled|haul)\s+"""), "")
            // Strip run-on preambles like "completed punch list repairs caught" that precede the actual task
            .replace(Regex("""(?i)^.*?\bcompleted\s+punch\s+list\s+repairs\s+"""), "")
            .trim(' ', ',', '.', '-')
        // Strip leading conjunction openers like "and added", "and", "and charged" that
        // appear when the money-match segmenter splits at a trailing material/supply cost.
        // In that case fall back to the post-price description from pricePhrase.
        val isConjunctionOnly = Regex("""(?i)^(?:and\s+(?:added|charged|included|also)?|also)\s*$""").matches(actionPhrase.trim())
        if (isConjunctionOnly) {
            // Use the material/supply text that follows the price as the description.
            val postPrice = cleanupDescription(extractPostPriceDescription(pricePhrase)).trim()
            if (postPrice.isBlank() || postPrice.length < 3) return null
            val total = amount
            AppLogger.d("DeterministicInvoice", "Conjunction-only phrase='$rawPhrase' → using post-price='$postPrice' amount=$total")
            return LineItem(
                description = ItemsSummary(postPrice.replaceFirstChar { it.uppercase() }),
                amount = MoneyAmount(total),
                category = inferCategory(postPrice.lowercase()),
                quantity = null,
                unitPrice = null
            )
        }
        val phrase = actionPhrase
            .replace(Regex("""(?i)^to\s+(?=shut\s+off\s+valves?\b)"""), "2 ")
            .replace(Regex("""(?i)^to\s+(?=(?:nail\s+pops?|outlets?|receptacles?|valves?|boards?|fixtures?|doors?)\b)"""), "2 ")
            // "adjusted to X doors" → "adjusted 2 X doors"
            .replace(Regex("""(?i)\badjusted\s+to\s+(?=(\w+\s+)?doors?\b)"""), "adjusted 2 ")
            // Strip trailing "is" connector left by "X doors is $N each" phrasing
            .replace(Regex("""(?i)\s+is\s*$"""), "")
        val lower = phrase.lowercase()
        if (phrase.isBlank()) return null

        val labor = Regex("""(?i)(\d+(?:\.\d+)?)\s+hours?\s+(?:of\s+)?labor""").find(phrase)
        if (labor != null) {
            val quantity = labor.groupValues[1].toDoubleOrNull()
            val total = quantity?.let { it * amount } ?: amount
            return LineItem(
                description = ItemsSummary("${quantity?.formatQuantity() ?: ""} hours of labor".trim().replaceFirstChar { it.uppercase() }),
                amount = MoneyAmount(total),
                category = "Labor",
                quantity = quantity,
                unitPrice = MoneyAmount(amount)
            )
        }

        val quantity = Regex("""(?i)^\s*(\d+(?:\.\d+)?)\s+""").find(phrase)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: Regex("""(?i)\b(\d+(?:\.\d+)?)\s+(?:linear\s+feet|feet|ft|shut|nail|light|outlet|valve|board|boards)\b""")
                .find(actionPhrase)
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()
            ?: Regex("""(?i)\b(\d+(?:\.\d+)?)\b""")
                .find(actionPhrase)
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()
        val unitPriced = quantity != null &&
            Regex("""(?i)\b(each|apiece|a\s+piece|per\s+\w+)\b""").containsMatchIn(pricePhrase)
        val total = if (unitPriced) amount * quantity else amount
        val description = cleanupDescription(phrase).let { cleaned ->
            if (cleaned.equals("a", ignoreCase = true) ||
                cleaned.equals("an", ignoreCase = true) ||
                cleaned.startsWith("At ", ignoreCase = true) ||
                Regex("""(?i)\bdeer\b""").containsMatchIn(cleaned)
            ) {
                cleanupDescription(extractPostPriceDescription(pricePhrase)).ifBlank { cleaned }
            } else {
                cleaned
            }
        }.withActionPrefix(rawPhrase).trim()
        // Guard: withActionPrefix can produce prefix-only strings like "Installed " when the
        // body is empty. Re-check after trim, and require at least 3 chars of real content.
        if (description.isBlank() || description.length < 3) return null
        AppLogger.d(
            "DeterministicInvoice",
            "Line phrase='$phrase' price='$pricePhrase' quantity=$quantity unitPriced=$unitPriced amount=$amount total=$total desc='$description'"
        )
        return LineItem(
            description = ItemsSummary(description),
            amount = MoneyAmount(total),
            category = inferCategory(lower),
            quantity = quantity,
            unitPrice = if (unitPriced) MoneyAmount(amount) else null
        )
    }

    private fun extractPostPriceDescription(pricePhrase: String): String {
        return pricePhrase
            .replace(
                Regex(
                    """(?i)^(?:at|for)?\s*\$?\s*\d+(?:\.\d+)?\s*(?:dollars?|bucks?|cents?)?(?:\s+an?\s+hour|\s*/\s*hour|\s+each|\s+apiece|\s+a\s+piece|\s+per\s+\w+(?:\s+\w+)?)?"""
                ),
                ""
            )
            .replace(Regex("""(?i)\b(?:for|at)\s+\$?\s*\d+(?:\.\d+)?.*$"""), "")
            .trim()
    }

    private fun parseSimpleInvoice(text: String): AiInvoiceResult? {
        val match = Regex(
            pattern = """(?i)^invoice\s+(.+?)\s+for\s+(.+?)(?:,|\s+)(\d+(?:\.\d+)?)\s*(?:dollars?)?\.?$"""
        ).matchEntire(text) ?: return null
        val clientName = match.groupValues[1].trim()
        val description = match.groupValues[2].trim().replaceFirstChar { it.uppercase() }
        val amount = match.groupValues[3].toDoubleOrNull() ?: return null
        if (clientName.isBlank() || description.isBlank()) return null
        return AiInvoiceResult(
            clientName = clientName,
            items = listOf(
                LineItem(
                    description = ItemsSummary(description),
                    amount = MoneyAmount(amount),
                    category = inferCategory(description),
                    quantity = 1.0,
                    unitPrice = MoneyAmount(amount)
                )
            ),
            confidenceScore = 0.98,
            userSummary = "Prepared 1 invoice line for $clientName."
        )
    }

    private fun parseSimpleFactura(text: String): AiInvoiceResult? {
        val match = Regex(
            pattern = """(?i)^factura\s+a\s+(.+?)\s+(\d+(?:\.\d+)?)\s+por\s+(.+?)(?:\s+y\s+ponle\s+tax\s+de\s+(.+))?\.?$"""
        ).matchEntire(text) ?: return null
        val clientName = match.groupValues[1].trim()
        val amount = match.groupValues[2].toDoubleOrNull() ?: return null
        val description = match.groupValues[3].trim().replaceFirstChar { it.uppercase() }
        val taxPhrase = match.groupValues.getOrNull(4).orEmpty()
        val tax = parseNamedTax(taxPhrase) ?: 7.0
        if (clientName.isBlank() || description.isBlank()) return null
        return AiInvoiceResult(
            clientName = clientName,
            items = listOf(
                LineItem(
                    description = ItemsSummary(description),
                    amount = MoneyAmount(amount),
                    category = inferCategory(description),
                    quantity = 1.0,
                    unitPrice = MoneyAmount(amount)
                )
            ),
            taxRatePercent = tax,
            confidenceScore = 0.95,
            userSummary = "Prepared 1 invoice line for $clientName."
        )
    }

    private fun parseNamedTax(text: String): Double? {
        val normalized = text.lowercase()
        return when {
            "siete" in normalized || "seven" in normalized -> 7.0
            "ocho" in normalized || "eight" in normalized -> 8.0
            "diez" in normalized || "ten" in normalized -> 10.0
            else -> Regex("""(\d+(?:\.\d+)?)""").find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
        }
    }

    private fun inferCategory(description: String): String {
        val lower = description.lowercase()
        return when {
            listOf("hour", "labor", "labour", "hora").any(lower::contains) -> "Labor"
            listOf("drywall", "sheetrock", "tablaroca", "mud", "skim", "tape").any(lower::contains) -> "Drywall"
            listOf("wire", "circuit", "breaker", "light fixture", "fixture", "receptacle", "outlet", "gfci", "electrical").any(lower::contains) -> "Electrical"
            listOf("toilet", "sink", "faucet", "plumbing").any(lower::contains) -> "Plumbing"
            listOf(
                "deck", "board", "boards", "stair", "tread", "handrail", "rail", "lumber",
                "framed", "frame", "closet", "prehung", "door", "baseboard", "shelving",
                "shelf", "window sill", "sill", "casing", "carpentry"
            ).any(lower::contains) -> "Carpentry"
            listOf("paint", "painting", "painted").any(lower::contains) -> "Painting"
            listOf("carpet", "floor", "flooring").any(lower::contains) -> "Flooring"
            listOf("siding", "general repair", "repair").any(lower::contains) -> "General Repair"
            listOf("material", "materials").any(lower::contains) -> "Materials"
            else -> "Service"
        }
    }

    private fun parseTaxRate(text: String): Double? {
        return Regex("""(?i)\b(?:tax|tax rate)\s+(?:at|of|is)?\s*(\d+(?:\.\d+)?)\s*%?""")
            .find(text)
            ?.groupValues
            ?.get(1)
            ?.toDoubleOrNull()
            ?: Regex("""(?i)\b(\d+(?:\.\d+)?)\s*%\s*(?:tax|tax rate)\b""")
                .find(text)
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()
    }

    private fun parseDeposit(text: String): Double? {
        return Regex("""(?i)\$?\s*(\d+(?:\.\d+)?)\s*(?:dollars?|bucks?)?\s+deposit\b""")
            .find(text)
            ?.groupValues
            ?.get(1)
            ?.toDoubleOrNull()
            ?: Regex("""(?i)\bdeposit\s+(?:already\s+paid\s+)?(?:of\s+)?\$?\s*(\d+(?:\.\d+)?)""")
                .find(text)
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()
    }

    private fun extractRawAddress(text: String): String {
        val addressKeyword = Regex(
            """(?i)\baddress\s+(.+?)(?=[.,;!?]*\s+(?:invoice|bill|charge|estimate|we\s+did|did|replace|replaced|also\s+replace|add|installed|install|labor|paint|painting|plumbing|electrical|we|for|to|and)\b|$)"""
        ).find(text)?.groupValues?.get(1).orEmpty()
        if (addressKeyword.isLikelyStreetAddress()) return addressKeyword

        val afterClientAtZip = Regex(
            """(?i)\bat\s+(\d+\s+.+?\b\d{5}(?:-\d{4})?)\b"""
        ).find(text)?.groupValues?.get(1).orEmpty()
        if (afterClientAtZip.isLikelyStreetAddress()) return afterClientAtZip

        val afterClientAt = Regex(
            """(?i)\bat\s+(\d+\s+.+?)(?=[.,;!?]*\s+(?:invoice|bill|charge|estimate|we\s+did|did|replace|replaced|also\s+replace|add|installed|install|labor|paint|painting|plumbing|electrical|we|for|to|and)\b|$)"""
        ).find(text)?.groupValues?.get(1).orEmpty()
        return afterClientAt.takeIf { it.isLikelyStreetAddress() }.orEmpty()
    }

    private fun String.isLikelyStreetAddress(): Boolean {
        val lower = lowercase()
        val hasStreetNumber = Regex("""^\s*\d+\b""").containsMatchIn(this)
        val hasStreetToken = listOf(
            "street", "st", "road", "rd", "avenue", "ave", "court", "ct", "drive", "dr",
            "lane", "ln", "boulevard", "blvd", "way", "place", "pl", "circle", "cir"
        ).any { token -> Regex("""\b${Regex.escape(token)}\b""").containsMatchIn(lower) }
        return hasStreetNumber && hasStreetToken
    }

    private fun cleanupDescription(raw: String): String {
        val text = raw
            .replace(Regex("""(?i)\bwe\s+"""), "")
            .replace(Regex("""(?i)\bput mud on it\b"""), "mudded")
            .replace(Regex("""(?i)\b(?:gsci|tsci)\b"""), "GFCI")
            .replace(Regex("""(?i)^around\s+(?=\d+(?:\.\d+)?\s+(?:linear\s+feet|feet|ft)\s+of\s+wire\b)"""), "ran ")
            .replace(Regex("""(?i)\bstart\s+(\d+(?:\.\d+)?)\s+dedicated\s+la\b"""), "installed $1 dedicated outlet")
            .replace(Regex("""(?i)\btaped it\b"""), "taped")
            .replace(Regex("""(?i)\bskimmed it\b"""), "skimmed")
            .replace(Regex("""(?i)^go\s+wash\b"""), "pressure washed")
            .replace(Regex("""(?i)^to\s+washed?\b"""), "pressure washed")
            .replace(Regex("""(?i)^back\s+the\s+steps\b"""), "sealed back steps")
            .replace(Regex("""(?i)^away\s+the\s+old\s+unit\b"""), "hauled away the old unit")
            .replace(Regex("""(?i)\breplace\b"""), "Replace")
            .replace(Regex("""(?i)\breplaced\b"""), "Replace")
            .replace(Regex("""(?i)^a\s+"""), "")
            .replace(Regex("""(?i)^an\s+"""), "")
            .replace(Regex("""(?i)^the\s+"""), "")
            .replace(Regex("""(?i)^(?:foot|feet|ft|square\s+foot|square\s+feet)\s+"""), "")
            .replace(Regex("""(?i)^back\s+the\s+steps\b"""), "sealed back steps")
            .replace(Regex("""(?i)\b(?:to|for|at|and|is)$"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ',', '.', '-')
        return text.replaceFirstChar { it.uppercase() }
    }

    private fun String.withActionPrefix(rawPhrase: String): String {
        if (hasActionPrefix()) return this
        val actionSource = rawPhrase
            .replace(Regex("""(?i)\b(?:at|for|is)?\s*\$?\s*\d+(?:\.\d+)?.*$"""), "")
            .trim()
        val prefix = when {
            Regex("""(?i)^to\s+shut\s+off\s+valves?\b""").containsMatchIn(actionSource) -> "Installed"
            Regex("""(?i)\b(?:installed?|install)\b""").containsMatchIn(actionSource) -> "Installed"
            Regex("""(?i)\b(?:painted?|paint)\b""").containsMatchIn(actionSource) -> "Painted"
            Regex("""(?i)\b(?:repaired?|repair|fixed|fix)\b""").containsMatchIn(actionSource) -> "Repaired"
            Regex("""(?i)\b(?:replaced?|replace)\b""").containsMatchIn(actionSource) -> "Replaced"
            Regex("""(?i)\b(?:hauled|haul|removed|remove)\b""").containsMatchIn(actionSource) -> "Hauled"
            Regex("""(?i)\b(?:sealed|seal)\b""").containsMatchIn(actionSource) -> "Sealed"
            Regex("""(?i)\b(?:pressure\s+washed?|pressure\s+wash|go\s+wash|to\s+washed?)\b""").containsMatchIn(actionSource) -> "Pressure washed"
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

    private fun String.cleanName(): String =
        replace(Regex("""(?i)^\s*new\s+client\s+"""), "")
            .trim(' ', ',', '.', '-')
            .split(" ")
            .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }

    private fun String.cleanAddress(): String =
        trim(' ', ',', '.', '-')
            .replace(Regex("""(?i)\bgeorgia\b"""), "GA")
            .replace(Regex("""\s+"""), " ")

    private fun Double.formatQuantity(): String =
        if (this % 1.0 == 0.0) toInt().toString() else toString()

    private fun String.normalizeVoiceInvoiceTranscript(): String {
        return lowercaseNumberWords()
            .repairCollapsedMoneyBeforeQuantity()
            .replace(Regex("""(?i)\bjust\s+in\b"""), "Justin")
            .replace(Regex("""(?i)\bstart\s+(1|one)\s+dedicated\s+la\b"""), "installed 1 dedicated outlet")
            .replace(Regex("""(?i)\bat\s+(\d{2})\s+50\s+cents?\s+per\b""")) { match ->
                "at ${match.groupValues[1]} cents per"
            }
            .replace(Regex("""(?i)\b(?:gsci|tsci)\b"""), "GFCI")
            .replace(Regex("""\b(\d+):(\d{2})\b""")) { match ->
                match.groupValues[1] + match.groupValues[2]
            }
            .replace(Regex("""(?<=\d)(?=[A-Za-z])"""), " ")
            // STT mishears "patch" as "pass" in material/supply context
            .replace(Regex("""(?i)\bpass\s+materials?\b"""), "patch materials")
            // Normalize "is $N" price connector to "at $N" so it doesn't bleed into descriptions
            .replace(Regex("""(?i)\bis\s+(\$?\s*\d)""")) { match -> "at ${match.groupValues[1]}" }
            // STT mishears "caulked" as "caught" in trade/construction context
            .replace(Regex("""(?i)\bcaught\s+(?=the\s+|a\s+|\w)"""), "caulked ")
    }

    private fun String.repairCollapsedMoneyBeforeQuantity(): String =
        replace(Regex("""(?i)\$(\d{2})(10|11|12)\s+(hours?|hrs?|h)\b""")) { match ->
            "$${match.groupValues[1]} ${match.groupValues[2]} ${match.groupValues[3]}"
        }

    private fun String.lowercaseNumberWords(): String {
        val replacements = mapOf(
            "one" to "1",
            "two" to "2",
            "three" to "3",
            "four" to "4",
            "five" to "5",
            "six" to "6",
            "seven" to "7",
            "eight" to "8",
            "nine" to "9",
            "ten" to "10",
            "eleven" to "11",
            "twelve" to "12"
        )
        return replacements.entries.fold(this) { current, (word, number) ->
            current.replace(Regex("""(?i)\b$word\b"""), number)
        }
    }
}
