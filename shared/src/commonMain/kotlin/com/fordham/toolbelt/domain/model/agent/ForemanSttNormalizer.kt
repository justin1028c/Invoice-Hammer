package com.fordham.toolbelt.domain.model.agent

/**
 * Normalizes voice transcripts before local routing (fillers, common STT homophones).
 */
object ForemanSttNormalizer {
    private val FILLER_PREFIX = Regex(
        """^(?:hey\s+foreman|hey\s+hammer|ok\s+foreman|okay\s+foreman|please\s+|can\s+you\s+|could\s+you\s+)+""",
        RegexOption.IGNORE_CASE
    )

    private val FILLER_SUFFIX = Regex(
        """(?:\s+please|\s+thanks|\s+thank\s+you)[.!?\s]*$""",
        RegexOption.IGNORE_CASE
    )

    private val HOMOPHONE_REPLACEMENTS = listOf(
        Regex("""\bknew invoice\b""", RegexOption.IGNORE_CASE) to "new invoice",
        Regex("""\bknew tab\b""", RegexOption.IGNORE_CASE) to "new tab",
        Regex("""\bgnu invoice\b""", RegexOption.IGNORE_CASE) to "new invoice",
        Regex("""\bshow me the\b""", RegexOption.IGNORE_CASE) to "show",
        Regex("""\btake me to the\b""", RegexOption.IGNORE_CASE) to "take me to",
        Regex("""\bgo to the\b""", RegexOption.IGNORE_CASE) to "go to",
        Regex("""\bopen the\b""", RegexOption.IGNORE_CASE) to "open",
        Regex("""\bcustomer\b""", RegexOption.IGNORE_CASE) to "client",
        Regex("""\bcustomers\b""", RegexOption.IGNORE_CASE) to "clients",
        Regex("""\bsupplier\b""", RegexOption.IGNORE_CASE) to "store",
        Regex("""\bsuppliers\b""", RegexOption.IGNORE_CASE) to "stores",
        Regex("""\bexpense\b""", RegexOption.IGNORE_CASE) to "receipt",
        Regex("""\bexpenses\b""", RegexOption.IGNORE_CASE) to "receipts",
    )

    fun normalize(raw: String): String {
        var text = raw.trim()
            .replace(Regex("""[.!?]+$"""), "")
            .replace(Regex("""\s+"""), " ")
        repeat(3) {
            text = text.replace(FILLER_PREFIX, "").trim()
        }
        text = text.replace(FILLER_SUFFIX, "").trim()
        for ((pattern, replacement) in HOMOPHONE_REPLACEMENTS) {
            text = text.replace(pattern, replacement)
        }
        return text.trim()
    }
}
