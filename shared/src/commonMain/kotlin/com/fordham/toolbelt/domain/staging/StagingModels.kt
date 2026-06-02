package com.fordham.toolbelt.domain.staging

import kotlin.jvm.JvmInline

// ── Value classes (Swift interop safe — no generics, no stdlib Result) ────────────────────────

@JvmInline 
value class FragmentText(val value: String)

/**
 * All monetary amounts inside the staging layer are stored as micro-cents (Long).
 * 1 dollar = 100_00 micro-cents (cents scaled by 100). Prevents Double precision loss 
 * during multi-fragment accumulation before the value reaches DraftLineItemInput.
 *
 * Conversion boundary: call [toDollars] only when handing off to domain invoice models.
 */
@JvmInline 
value class MicroCentAmount(val value: Long) {

    fun toDollars(): Double = value / 10000.0

    companion object {
        private const val MICRO_CENT_SCALE = 10000.0
        private const val CENTS_TO_MICRO_CENTS_SCALE = 100L

        fun fromDollars(dollars: Double): MicroCentAmount =
            MicroCentAmount((dollars * MICRO_CENT_SCALE).toLong())

        /**
         * Parses a spoken or STT-produced number string into micro-cents.
         *
         * Handles:
         *   "800"        → $800.00
         *   "65.50"      → $65.50
         *   "65 50"      → $65.50  (STT two-token decimal: "sixty five fifty")
         *   "1200"       → $1200.00
         *
         * Does NOT handle verbal number words ("sixty five dollars") — those are
         * resolved upstream by Gemini's PARSE_VOICE_FRAGMENT task.
         */
        fun fromSpokenString(raw: String): MicroCentAmount? {
            val trimmed = raw.trim()

            // Standard decimal: "65.50" or "800.0"
            trimmed.toDoubleOrNull()?.let { return fromDollars(it) }

            // Two-token STT pattern: "65 50" → treat second token as cents
            val parts = trimmed.split(Regex("""\s+"""))
            if (parts.size == 2) {
                val dollars = parts[0].toLongOrNull()
                val cents = parts[1].toLongOrNull()
                if (dollars != null && cents != null && cents in 0..99) {
                    val centsValue = if (parts[1].length == 1) cents * 10 else cents
                    val totalMicroCents = (dollars * 10000L) + (centsValue * CENTS_TO_MICRO_CENTS_SCALE)
                    return MicroCentAmount(totalMicroCents)
                }
            }
            return null
        }
    }
}
