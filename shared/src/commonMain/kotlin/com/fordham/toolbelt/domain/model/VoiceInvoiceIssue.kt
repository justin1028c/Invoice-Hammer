package com.fordham.toolbelt.domain.model

enum class VoiceInvoiceIssueSeverity {
    Repairable,
    NeedsConfirmation,
    Block
}

data class VoiceInvoiceIssue(
    val code: String,
    val severity: VoiceInvoiceIssueSeverity,
    val message: String
)

object VoiceInvoiceIssueCatalog {
    fun classify(code: String): VoiceInvoiceIssue {
        val severity = when {
            code in BlockingCodes -> VoiceInvoiceIssueSeverity.Block
            code.startsWith("ITEM_") && code.contains("_ABSURD_") -> VoiceInvoiceIssueSeverity.Block
            code in ConfirmationCodes -> VoiceInvoiceIssueSeverity.NeedsConfirmation
            code.startsWith("ITEM_") -> VoiceInvoiceIssueSeverity.Repairable
            else -> VoiceInvoiceIssueSeverity.NeedsConfirmation
        }
        return VoiceInvoiceIssue(
            code = code,
            severity = severity,
            message = userMessageFor(code, severity)
        )
    }

    fun classifyAll(codes: List<String>): List<VoiceInvoiceIssue> =
        codes.distinct().map(::classify)

    private fun userMessageFor(code: String, severity: VoiceInvoiceIssueSeverity): String = when {
        code == "MISSING_CLIENT_NAME" -> "I need the client name before I can make the invoice."
        code == "NO_VALID_LINE_ITEMS" || code == "NO_LINE_ITEMS" || code == "PLAN_HAS_NO_LINE_ITEMS" ->
            "I need at least one clear line item with an amount."
        code == "ZERO_AMOUNT" -> "I heard a line item without a usable amount."
        code == "MISSING_CLIENT_ADDRESS" -> "The client address is missing."
        code == "UNMATCHED_MONEY_AMOUNT" -> "I heard a dollar amount that was not matched to an invoice line."
        code == "LOW_AUDIO_CONFIDENCE" -> "The audio was unclear, so I need confirmation."
        code == "MATH_MISMATCH" -> "The quantity and price do not match the total."
        code.contains("DESCRIPTION_TOO_SHORT") -> "A line item description was too short for a client-facing invoice."
        code.contains("RISKY_DESCRIPTION") -> "A line item description sounded like transcription noise."
        code.contains("LOOKS_LIKE_ADDRESS") -> "A parsed field looked like an address in the wrong place."
        code.contains("ABSURD") -> "A line item had an unrealistic quantity or amount."
        severity == VoiceInvoiceIssueSeverity.Block -> "I need more information before I can make the invoice."
        severity == VoiceInvoiceIssueSeverity.Repairable -> "I can repair this, but it should be reviewed."
        else -> "This invoice should be confirmed before applying."
    }

    private val BlockingCodes = setOf(
        "MISSING_CLIENT_NAME",
        "NO_VALID_LINE_ITEMS",
        "NO_LINE_ITEMS",
        "PLAN_HAS_NO_LINE_ITEMS",
        "ZERO_AMOUNT",
        "CLIENT_NAME_LOOKS_LIKE_ADDRESS",
        "REPEATED_TEXT_LOOP"
    )

    private val ConfirmationCodes = setOf(
        "MISSING_CLIENT_ADDRESS",
        "UNMATCHED_MONEY_AMOUNT",
        "TAX_RATE_MISMATCH",
        "MATH_MISMATCH",
        "LOW_AUDIO_CONFIDENCE",
        "CONTACT_PHONE_DETECTED",
        "CONTACT_EMAIL_DETECTED",
        "PLAN_REQUIRES_FOLLOW_UP"
    )
}
