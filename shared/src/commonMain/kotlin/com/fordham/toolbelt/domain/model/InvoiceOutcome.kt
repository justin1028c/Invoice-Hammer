package com.fordham.toolbelt.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class FailureMessage(val value: String)

sealed interface InvoiceOutcome {
    data object Success : InvoiceOutcome
    data class Failure(val error: FailureMessage) : InvoiceOutcome
}

sealed interface SaveInvoiceFailure {
    val message: FailureMessage

    data class ValidationFailure(override val message: FailureMessage) : SaveInvoiceFailure
    data class PersistenceFailure(override val message: FailureMessage) : SaveInvoiceFailure
    data class UnexpectedFailure(override val message: FailureMessage) : SaveInvoiceFailure
}

sealed interface SaveInvoiceOutcome {
    data class Success(val invoice: Invoice) : SaveInvoiceOutcome
    data class Error(val failure: SaveInvoiceFailure) : SaveInvoiceOutcome {
        val message: String get() = failure.message.value
    }
}

sealed interface GenerateInvoiceFailure {
    val message: FailureMessage

    data class ValidationFailure(override val message: FailureMessage) : GenerateInvoiceFailure
    data class PdfGenerationFailure(override val message: FailureMessage) : GenerateInvoiceFailure
    data class PersistenceFailure(override val message: FailureMessage) : GenerateInvoiceFailure
    data class UnexpectedFailure(override val message: FailureMessage) : GenerateInvoiceFailure
}

sealed interface GenerateInvoiceOutcome {
    data object Success : GenerateInvoiceOutcome
    data class Error(val failure: GenerateInvoiceFailure) : GenerateInvoiceOutcome {
        val message: String get() = failure.message.value
    }
}
