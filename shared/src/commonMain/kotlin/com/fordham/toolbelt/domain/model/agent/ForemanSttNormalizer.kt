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

    private fun replaceNumberWords(input: String): String {
        val wordsMap = mapOf(
            "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
            "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
            "ten" to "10", "eleven" to "11", "twelve" to "12", "thirteen" to "13",
            "fourteen" to "14", "fifteen" to "15", "sixteen" to "16", "seventeen" to "17",
            "eighteen" to "18", "nineteen" to "19", "twenty" to "20", "thirty" to "30",
            "forty" to "40", "fifty" to "50", "sixty" to "60", "seventy" to "70",
            "eighty" to "80", "ninety" to "90", "hundred" to "100"
        )
        val tokens = input.split(" ")
        val newTokens = tokens.map { token ->
            val cleanWord = token.lowercase().replace(Regex("""[^a-z0-9]"""), "")
            if (wordsMap.containsKey(cleanWord)) {
                token.replace(cleanWord, wordsMap[cleanWord]!!, ignoreCase = true)
            } else {
                token
            }
        }
        var result = newTokens.joinToString(" ")
        val prefixes = listOf("20", "30", "40", "50", "60", "70", "80", "90")
        for (prefix in prefixes) {
            for (i in 1..9) {
                val prefixWord = wordsMap.entries.first { it.value == prefix }.key
                val suffixWord = wordsMap.entries.first { it.value == i.toString() }.key
                result = result.replace("$prefix $i", "${prefix.toInt() + i}")
                result = result.replace(Regex("""\b$prefixWord-$suffixWord\b""", RegexOption.IGNORE_CASE), "${prefix.toInt() + i}")
                result = result.replace(Regex("""\b$prefixWord\s+$suffixWord\b""", RegexOption.IGNORE_CASE), "${prefix.toInt() + i}")
            }
        }
        return result
    }

    fun normalize(raw: String): String {
        var text = replaceNumberWords(raw.trim())
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
