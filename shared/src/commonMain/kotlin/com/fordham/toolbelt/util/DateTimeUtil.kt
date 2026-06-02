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
        return "${dt.monthNumber}/${dt.dayOfMonth}/${dt.year}"
    }

    fun formatMoney(amount: Double): String {
        val sign = if (amount < 0) "-" else ""
        val absAmount = if (amount < 0) -amount else amount
        return "$sign$${formatDecimal(absAmount, 2)}"
    }

    fun formatDecimal(value: Double, decimals: Int): String {
        val multiplier = 10.0.pow(decimals)
        val roundedValue = kotlin.math.round(value * multiplier) / multiplier
        val parts = roundedValue.toString().split(".")
        val whole = parts[0]
        var fraction = if (parts.size > 1) parts[1] else ""
        while (fraction.length < decimals) fraction += "0"
        if (fraction.length > decimals) fraction = fraction.substring(0, decimals)
        return if (decimals > 0) "$whole.$fraction" else whole
    }

    fun getNowFormatted(): String = formatEpoch(nowEpochMillis())

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
