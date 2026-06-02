package com.fordham.toolbelt.domain.model.cardterminal

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest

enum class CardBrand {
    Unknown,
    Visa,
    Mastercard,
    Amex,
    Discover
}

enum class CardTerminalPhase {
    Idle,
    SecuringConnection,
    Verifying,
    Settling,
    Success,
    Failed
}

data class CardTerminalDraft(
    val panDigits: String = "",
    val expiryInput: String = "",
    val cvvDigits: String = "",
    val cardholderName: String = ""
)

sealed interface CardTerminalValidationOutcome {
    data object Valid : CardTerminalValidationOutcome
    data class Invalid(val message: FailureMessage) : CardTerminalValidationOutcome
}

sealed interface CardTerminalPaymentOutcome {
    data class Success(val request: InvoicePaymentRequest) : CardTerminalPaymentOutcome
    data class Failure(val error: FailureMessage) : CardTerminalPaymentOutcome
}

val CardTerminalPhase.phaseLabel: String
    get() = when (this) {
        CardTerminalPhase.Idle -> ""
        CardTerminalPhase.SecuringConnection -> "Securing connection…"
        CardTerminalPhase.Verifying -> "Verifying card…"
        CardTerminalPhase.Settling -> "Settling funds…"
        CardTerminalPhase.Success -> "Payment approved"
        CardTerminalPhase.Failed -> "Payment failed"
    }
