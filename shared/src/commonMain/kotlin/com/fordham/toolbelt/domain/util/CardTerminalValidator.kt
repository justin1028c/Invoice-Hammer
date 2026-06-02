package com.fordham.toolbelt.domain.util

import com.fordham.toolbelt.domain.model.FailureMessage
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.fordham.toolbelt.domain.model.cardterminal.CardBrand
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalDraft
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalValidationOutcome

object CardTerminalValidator {

    fun validate(draft: CardTerminalDraft): CardTerminalValidationOutcome {
        val pan = draft.panDigits.filter { it.isDigit() }
        if (pan.length < 13 || pan.length > 19) {
            return CardTerminalValidationOutcome.Invalid(FailureMessage("Enter a valid card number."))
        }
        if (!passesLuhn(pan)) {
            return CardTerminalValidationOutcome.Invalid(FailureMessage("Card number failed verification."))
        }

        val brand = detectBrand(pan)
        if (!isExpiryValid(draft.expiryInput)) {
            return CardTerminalValidationOutcome.Invalid(FailureMessage("Expiry must be MM/YY and not in the past."))
        }

        val cvv = draft.cvvDigits.filter { it.isDigit() }
        val cvvLength = if (brand == CardBrand.Amex) 4 else 3
        if (cvv.length != cvvLength) {
            return CardTerminalValidationOutcome.Invalid(
                FailureMessage("Enter a ${cvvLength}-digit security code.")
            )
        }

        if (draft.cardholderName.trim().length < 2) {
            return CardTerminalValidationOutcome.Invalid(FailureMessage("Enter the cardholder name."))
        }

        return CardTerminalValidationOutcome.Valid
    }

    fun detectBrand(pan: String): CardBrand {
        val digits = pan.filter { it.isDigit() }
        return when {
            digits.startsWith("4") -> CardBrand.Visa
            digits.length >= 2 && digits.substring(0, 2).toIntOrNull() in 51..55 -> CardBrand.Mastercard
            digits.length >= 4 && digits.substring(0, 4).toIntOrNull() in 2221..2720 -> CardBrand.Mastercard
            digits.startsWith("34") || digits.startsWith("37") -> CardBrand.Amex
            digits.startsWith("6011") || digits.startsWith("65") -> CardBrand.Discover
            else -> CardBrand.Unknown
        }
    }

    fun passesLuhn(pan: String): Boolean {
        val digits = pan.filter { it.isDigit() }
        if (digits.isEmpty()) return false
        var sum = 0
        var alternate = false
        for (i in digits.indices.reversed()) {
            var n = digits[i].digitToInt()
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    fun formatPanDisplay(digits: String): String =
        digits.filter { it.isDigit() }
            .chunked(4)
            .joinToString(" ")

    fun formatExpiryInput(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(4)
        return when {
            digits.length <= 2 -> digits
            else -> "${digits.take(2)}/${digits.drop(2)}"
        }
    }

    private fun isExpiryValid(expiryInput: String): Boolean {
        val digits = expiryInput.filter { it.isDigit() }
        if (digits.length != 4) return false
        val month = digits.take(2).toIntOrNull() ?: return false
        val year = digits.drop(2).toIntOrNull() ?: return false
        if (month !in 1..12) return false
        val fullYear = 2000 + year
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        if (fullYear < today.year) return false
        if (fullYear == today.year && month < today.monthNumber) return false
        return true
    }
}
