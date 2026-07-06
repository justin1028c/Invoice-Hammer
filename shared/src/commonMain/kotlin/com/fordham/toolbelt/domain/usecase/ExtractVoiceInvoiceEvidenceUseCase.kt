package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.VoiceInvoiceEvidence

class ExtractVoiceInvoiceEvidenceUseCase {
    operator fun invoke(text: String): VoiceInvoiceEvidence {
        val normalized = text
            .normalizeNumberWords()
            .replace(Regex("\\s+"), " ")
            .trim()

        return VoiceInvoiceEvidence(
            normalizedTranscript = normalized,
            moneyAmounts = extractMoneyAmounts(normalized),
            percentages = extractPercentages(normalized),
            phoneNumbers = PhoneRegex.findAll(normalized).map { it.value.trim() }.distinct().toList(),
            emails = EmailRegex.findAll(normalized).map { it.value.trim() }.distinct().toList(),
            zipCodes = ZipRegex.findAll(normalized).map { it.value.trim() }.distinct().toList(),
            streetAddressCandidates = StreetAddressRegex.findAll(normalized)
                .map { it.value.trim(' ', ',', '.') }
                .distinct()
                .toList(),
            measurements = MeasurementRegex.findAll(normalized).map { it.value.trim() }.distinct().toList()
        )
    }

    private fun extractMoneyAmounts(text: String): List<Double> {
        val matches = MoneyRegex.findAll(text).mapNotNull { match ->
            match.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.replace(",", "")?.toDoubleOrNull()
        }
        return matches.toList()
    }

    private fun extractPercentages(text: String): List<Double> {
        return PercentRegex.findAll(text).mapNotNull {
            it.groupValues[1].toDoubleOrNull()
        }.toList()
    }

    private fun String.normalizeNumberWords(): String {
        var current = this
        NumberPhraseRegex.findAll(this).toList().asReversed().forEach { match ->
            val value = parseNumberPhrase(match.value)
            if (value != null) {
                current = current.replaceRange(match.range, value.formatNumber())
            }
        }
        return current
    }

    private fun parseNumberPhrase(phrase: String): Double? {
        val tokens = phrase.lowercase()
            .replace("-", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it != "and" }
        if (tokens.isEmpty()) return null

        var total = 0
        var current = 0
        for (token in tokens) {
            when (token) {
                in SmallNumbers -> current += SmallNumbers.getValue(token)
                in TensNumbers -> current += TensNumbers.getValue(token)
                "hundred" -> current = (if (current == 0) 1 else current) * 100
                "thousand" -> {
                    total += (if (current == 0) 1 else current) * 1000
                    current = 0
                }
                else -> return null
            }
        }
        return (total + current).takeIf { it > 0 }?.toDouble()
    }

    private fun Double.formatNumber(): String =
        if (this % 1.0 == 0.0) toInt().toString() else toString()

    private companion object {
        val MoneyRegex = Regex(
            """(?i)(?:\$\s*([0-9][0-9,]*(?:\.\d{1,2})?)|([0-9][0-9,]*(?:\.\d{1,2})?)\s*(?:dollars?|bucks?))"""
        )
        val PercentRegex = Regex("""(?i)\b(\d+(?:\.\d+)?)\s*(?:%|percent|per cent)\b""")
        val PhoneRegex = Regex("""(?x)(?:\+?1[\s.-]?)?(?:\(?\d{3}\)?[\s.-]?)\d{3}[\s.-]?\d{4}""")
        val EmailRegex = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
        val ZipRegex = Regex("""\b\d{5}(?:-\d{4})?\b""")
        val StreetAddressRegex = Regex(
            """(?i)\b\d+\s+[A-Za-z0-9 .'-]+?\s+(?:street|st|road|rd|avenue|ave|court|ct|drive|dr|lane|ln|boulevard|blvd|way|place|pl|circle|cir)\b(?:\s+[A-Za-z .'-]+)?(?:\s+\d{5}(?:-\d{4})?)?"""
        )
        val MeasurementRegex = Regex("""(?i)\b\d+(?:\.\d+)?\s*(?:ft|feet|foot|sq\s*ft|square\s+feet|yards?|yds?|lbs?|pounds?|hours?|hrs?)\b""")
        val NumberPhraseRegex = Regex(
            """(?i)\b(?:(?:zero|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety|hundred|thousand)(?:[\s-]+|$|and\s+))+"""
        )
        val SmallNumbers = mapOf(
            "zero" to 0,
            "one" to 1,
            "two" to 2,
            "three" to 3,
            "four" to 4,
            "five" to 5,
            "six" to 6,
            "seven" to 7,
            "eight" to 8,
            "nine" to 9,
            "ten" to 10,
            "eleven" to 11,
            "twelve" to 12,
            "thirteen" to 13,
            "fourteen" to 14,
            "fifteen" to 15,
            "sixteen" to 16,
            "seventeen" to 17,
            "eighteen" to 18,
            "nineteen" to 19
        )
        val TensNumbers = mapOf(
            "twenty" to 20,
            "thirty" to 30,
            "forty" to 40,
            "fifty" to 50,
            "sixty" to 60,
            "seventy" to 70,
            "eighty" to 80,
            "ninety" to 90
        )
    }
}
