package com.fordham.toolbelt.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DateTimeUtil {
    fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
    
    fun formatEpoch(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return if (AppLocale.fromSystem() == AppLocale.Spanish) {
            "${dt.dayOfMonth.toString().padStart(2, '0')}/" +
                "${dt.monthNumber.toString().padStart(2, '0')}/${dt.year}"
        } else {
            "${dt.monthNumber}/${dt.dayOfMonth}/${dt.year}"
        }
    }

    fun formatMoney(amount: Double): String {
        val sign = if (amount < 0) "-" else ""
        val absAmount = if (amount < 0) -amount else amount
        return "$sign$${formatDecimal(absAmount, 2)}"
    }

    fun formatDecimal(value: Double, decimals: Int): String {
        if (value.isNaN() || value.isInfinite()) return value.toString()
        val sign = if (value < 0.0) "-" else ""
        val absValue = kotlin.math.abs(value)
        val multiplier = 10.0.pow(decimals)
        val cents = kotlin.math.round(absValue * multiplier).toLong()
        val divisor = multiplier.toLong()
        val whole = if (divisor > 0L) cents / divisor else cents
        val fraction = if (divisor > 0L) cents % divisor else 0L
        val fractionStr = if (decimals > 0) fraction.toString().padStart(decimals, '0') else ""
        return if (decimals > 0) "$sign$whole.$fractionStr" else "$sign$whole"
    }

    fun getNowFormatted(): String = formatEpoch(nowEpochMillis())

    fun parseDate(dateStr: String): kotlinx.datetime.LocalDate? {
        val trimmed = dateStr.trim()
        if (trimmed.isEmpty()) return null

        // Try YYYY-MM-DD
        if (trimmed.contains("-")) {
            val parts = trimmed.split("-")
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                val d = parts[2].toIntOrNull()
                if (y != null && m != null && d != null) {
                    try {
                        return kotlinx.datetime.LocalDate(y, m, d)
                    } catch (e: Exception) {}
                }
            }
        }

        // Try M/D/YYYY or DD/MM/YYYY
        if (trimmed.contains("/")) {
            val parts = trimmed.split("/")
            if (parts.size == 3) {
                val p0 = parts[0].toIntOrNull()
                val p1 = parts[1].toIntOrNull()
                val y = parts[2].toIntOrNull()
                if (p0 != null && p1 != null && y != null) {
                    try {
                        if (p0 > 12) {
                            return kotlinx.datetime.LocalDate(y, p1, p0)
                        } else if (p1 > 12) {
                            return kotlinx.datetime.LocalDate(y, p0, p1)
                        } else {
                            val isSpanish = AppLocale.fromSystem() == AppLocale.Spanish
                            if (isSpanish) {
                                return kotlinx.datetime.LocalDate(y, p1, p0)
                            } else {
                                return kotlinx.datetime.LocalDate(y, p0, p1)
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
        }

        // Try Month Names like "Jan 01, 2026", "Feb 15, 2026"
        val monthMap = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
            "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
            "ene" to 1, "abr" to 4, "ago" to 8, "dic" to 12,
            "january" to 1, "february" to 2, "march" to 3, "april" to 4, "june" to 6,
            "july" to 7, "august" to 8, "september" to 9, "october" to 10, "november" to 11, "december" to 12
        )

        val cleaned = trimmed.replace(",", " ").replace(Regex("\\s+"), " ")
        val parts = cleaned.split(" ")
        if (parts.size == 3) {
            val p0 = parts[0].lowercase()
            val p1 = parts[1].lowercase()
            val y = parts[2].toIntOrNull()
            if (y != null) {
                val m0 = monthMap[p0]
                val m1 = monthMap[p1]
                if (m0 != null) {
                    val d = parts[1].toIntOrNull()
                    if (d != null) {
                        try { return kotlinx.datetime.LocalDate(y, m0, d) } catch (e: Exception) {}
                    }
                } else if (m1 != null) {
                    val d = parts[0].toIntOrNull()
                    if (d != null) {
                        try { return kotlinx.datetime.LocalDate(y, m1, d) } catch (e: Exception) {}
                    }
                }
            }
        }

        return null
    }


    /**
     * Displays a stored invoice date. Converts legacy M/D/Y values to D/M/Y for Spanish locale.
     */
    fun formatDateForDisplay(stored: String): String {
        if (AppLocale.fromSystem() != AppLocale.Spanish) return stored
        val parts = stored.trim().split("/")
        if (parts.size != 3) return stored
        val first = parts[0].toIntOrNull() ?: return stored
        val second = parts[1].toIntOrNull() ?: return stored
        val year = parts[2]
        if (first in 1..12 && second in 1..31) {
            return "${second.toString().padStart(2, '0')}/" +
                "${first.toString().padStart(2, '0')}/$year"
        }
        return stored
    }

    /** Filesystem-safe stamp for export filenames, e.g. `20260519_143052`. */
    fun exportFileStamp(): String {
        val dt = Instant.fromEpochMilliseconds(nowEpochMillis()).toLocalDateTime(TimeZone.currentSystemDefault())
        val month = dt.monthNumber.toString().padStart(2, '0')
        val day = dt.dayOfMonth.toString().padStart(2, '0')
        val hour = dt.hour.toString().padStart(2, '0')
        val minute = dt.minute.toString().padStart(2, '0')
        val second = dt.second.toString().padStart(2, '0')
        return "${dt.year}${month}${day}_${hour}${minute}${second}"
    }
}

private fun Double.pow(n: Int): Double {
    var result = 1.0
    repeat(n) { result *= this }
    return result
}
