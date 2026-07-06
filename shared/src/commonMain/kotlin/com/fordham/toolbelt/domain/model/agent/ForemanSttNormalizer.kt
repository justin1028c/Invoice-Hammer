package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.util.AppLocale

/**
 * Normalizes voice transcripts before local routing (fillers, common STT homophones).
 *
 * FIX (Spanish voice-routing gap): previously this only recognized English fillers,
 * homophones, and number words, so every Spanish command fell through to the LLM path
 * and lost the local fast-path entirely. This now branches on AppLocale so English
 * behavior is byte-for-byte unchanged, and Spanish gets an equivalent treatment.
 */
object ForemanSttNormalizer {
    // ---------- English (unchanged) ----------
    private val FILLER_PREFIX_EN = Regex(
        """^(?:hey\s+foreman|hey\s+hammer|ok\s+foreman|okay\s+foreman|please\s+|can\s+you\s+|could\s+you\s+)+""",
        RegexOption.IGNORE_CASE
    )

    private val FILLER_SUFFIX_EN = Regex(
        """(?:\s+please|\s+thanks|\s+thank\s+you)[.!?\s]*$""",
        RegexOption.IGNORE_CASE
    )

    private val HOMOPHONE_REPLACEMENTS_EN = listOf(
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
        Regex("""\bexpenses\b""", RegexOption.IGNORE_CASE) to "receipts"
    )

    private val NUMBER_WORDS_EN = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
        "ten" to "10", "eleven" to "11", "twelve" to "12", "thirteen" to "13",
        "fourteen" to "14", "fifteen" to "15", "sixteen" to "16", "seventeen" to "17",
        "eighteen" to "18", "nineteen" to "19", "twenty" to "20", "thirty" to "30",
        "forty" to "40", "fifty" to "50", "sixty" to "60", "seventy" to "70",
        "eighty" to "80", "ninety" to "90", "hundred" to "100"
    )

    private val TENS_EN = listOf("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

    // ---------- Spanish (new) ----------
    private val FILLER_PREFIX_ES = Regex(
        """^(?:oye\s+(?:foreman|hammer)|ok(?:ay)?\s+(?:foreman|hammer)|por\s+favor\s+|puedes\s+|podrías\s+|podrias\s+)+""",
        RegexOption.IGNORE_CASE
    )

    private val FILLER_SUFFIX_ES = Regex(
        """(?:\s+por\s+favor|\s+gracias)[.!?\s]*$""",
        RegexOption.IGNORE_CASE
    )

    private val HOMOPHONE_REPLACEMENTS_ES = listOf(
        Regex("""\bconsumidor\b""", RegexOption.IGNORE_CASE) to "cliente",
        Regex("""\bconsumidores\b""", RegexOption.IGNORE_CASE) to "clientes",
        Regex("""\bproveedor\b""", RegexOption.IGNORE_CASE) to "tienda",
        Regex("""\bproveedores\b""", RegexOption.IGNORE_CASE) to "tiendas",
        Regex("""\bmuéstrame\b""", RegexOption.IGNORE_CASE) to "muestra",
        Regex("""\bllévame a\b""", RegexOption.IGNORE_CASE) to "ve a",
        Regex("""\bllevame a\b""", RegexOption.IGNORE_CASE) to "ve a"
    )

    // "veintiuno".."veintinueve" and "dieciséis".."diecinueve" are fused single words in Spanish;
    // handled as literal entries rather than via the tens-loop used for 21-99 elsewhere.
    private val NUMBER_WORDS_ES = mapOf(
        "cero" to "0", "uno" to "1", "una" to "1", "dos" to "2", "tres" to "3", "cuatro" to "4",
        "cinco" to "5", "seis" to "6", "siete" to "7", "ocho" to "8", "nueve" to "9",
        "diez" to "10", "once" to "11", "doce" to "12", "trece" to "13", "catorce" to "14",
        "quince" to "15", "dieciseis" to "16", "dieciséis" to "16", "diecisiete" to "17",
        "dieciocho" to "18", "diecinueve" to "19", "veinte" to "20",
        "veintiuno" to "21", "veintidos" to "22", "veintidós" to "22", "veintitres" to "23",
        "veintitrés" to "23", "veinticuatro" to "24", "veinticinco" to "25", "veintiseis" to "26",
        "veintiséis" to "26", "veintisiete" to "27", "veintiocho" to "28", "veintinueve" to "29",
        "treinta" to "30", "cuarenta" to "40", "cincuenta" to "50", "sesenta" to "60",
        "setenta" to "70", "ochenta" to "80", "noventa" to "90", "cien" to "100", "ciento" to "100"
    )

    private val TENS_ES = listOf("treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa")

    private fun replaceNumberWords(input: String, isSpanish: Boolean): String {
        val wordsMap = if (isSpanish) NUMBER_WORDS_ES else NUMBER_WORDS_EN
        val tokens = input.split(" ")
        val newTokens = tokens.map { token ->
            val cleanWord = token.lowercase().replace(Regex("""[^a-záéíóúñ0-9]"""), "")
            if (wordsMap.containsKey(cleanWord)) {
                token.replace(cleanWord, wordsMap[cleanWord]!!, ignoreCase = true)
            } else {
                token
            }
        }
        var result = newTokens.joinToString(" ")
        
        if (isSpanish) {
            // "treinta y cinco" -> "35" (Spanish joins tens+units with "y")
            for (tensWord in TENS_ES) {
                val tensValue = NUMBER_WORDS_ES.getValue(tensWord).toInt()
                for (i in 1..9) {
                    val unitWord = NUMBER_WORDS_ES.entries.first { it.value == i.toString() && it.key !in listOf("una") }.key
                    result = result.replace(
                        Regex("""\b$tensWord\s+y\s+$unitWord\b""", RegexOption.IGNORE_CASE),
                        "${tensValue + i}"
                    )
                    result = result.replace("$tensValue $i", "${tensValue + i}")
                }
            }
        } else {
            // "twenty two" / "twenty-two" -> "22"
            val prefixes = listOf("20", "30", "40", "50", "60", "70", "80", "90")
            for (prefix in prefixes) {
                for (i in 1..9) {
                    val prefixWord = TENS_EN.first { NUMBER_WORDS_EN.getValue(it) == prefix }
                    val suffixWord = NUMBER_WORDS_EN.entries.first { it.value == i.toString() }.key
                    result = result.replace("$prefix $i", "${prefix.toInt() + i}")
                    result = result.replace(
                        Regex("""\b$prefixWord-$suffixWord\b""", RegexOption.IGNORE_CASE),
                        "${prefix.toInt() + i}"
                    )
                    result = result.replace(
                        Regex("""\b$prefixWord\s+$suffixWord\b""", RegexOption.IGNORE_CASE),
                        "${prefix.toInt() + i}"
                    )
                }
            }
        }
        return result
    }

    fun normalize(raw: String): String {
        val isSpanish = AppLocale.fromSystem() == AppLocale.Spanish
        var text = replaceNumberWords(raw.trim(), isSpanish)
            .replace(Regex("""[.!?]+$"""), "")
            .replace(Regex("""\s+"""), " ")
        
        val fillerPrefix = if (isSpanish) FILLER_PREFIX_ES else FILLER_PREFIX_EN
        val fillerSuffix = if (isSpanish) FILLER_SUFFIX_ES else FILLER_SUFFIX_EN
        val homophones = if (isSpanish) HOMOPHONE_REPLACEMENTS_ES else HOMOPHONE_REPLACEMENTS_EN
        
        repeat(3) {
            text = text.replace(fillerPrefix, "").trim()
        }
        text = text.replace(fillerSuffix, "").trim()
        for ((pattern, replacement) in homophones) {
            text = text.replace(pattern, replacement)
        }
        return text.trim()
    }
}
