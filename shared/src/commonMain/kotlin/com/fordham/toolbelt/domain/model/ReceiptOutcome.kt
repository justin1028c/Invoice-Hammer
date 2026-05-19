package com.fordham.toolbelt.domain.model

sealed interface ReceiptOutcome {
    data object Success : ReceiptOutcome
    data class Failure(val error: FailureMessage) : ReceiptOutcome
}

sealed interface ReceiptListOutcome {
    data class Success(val receipts: List<ReceiptItem>) : ReceiptListOutcome
    data class Failure(val error: FailureMessage) : ReceiptListOutcome
}

sealed interface ProcessReceiptOutcome {
    data class Success(val items: List<ReceiptItem>) : ProcessReceiptOutcome
    data class Failure(val error: FailureMessage) : ProcessReceiptOutcome
    data object PremiumRequired : ProcessReceiptOutcome
}
