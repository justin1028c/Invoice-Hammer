package com.fordham.toolbelt.util

import java.math.BigDecimal

object OcrParser {
    // Matches digits followed by a period and exactly two digits (e.g., 12.99, 1045.00)
    private val CURRENCY_REGEX = Regex("""\b\d+\.\d{2}\b""")

    fun extractMaximumPrice(rawText: String): BigDecimal {
        return CURRENCY_REGEX.findAll(rawText)
            .mapNotNull { it.value.toBigDecimalOrNull() }
            .maxOrNull() ?: BigDecimal.ZERO
    }
}
