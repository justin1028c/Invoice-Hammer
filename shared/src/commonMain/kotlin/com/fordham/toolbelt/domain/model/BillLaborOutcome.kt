package com.fordham.toolbelt.domain.model

sealed interface BillLaborFailure {
    val message: FailureMessage

    data class DraftUpdateFailure(override val message: FailureMessage) : BillLaborFailure
    data class UnexpectedFailure(override val message: FailureMessage) : BillLaborFailure
}

sealed interface BillLaborOutcome {
    data object Success : BillLaborOutcome
    data class Error(val failure: BillLaborFailure) : BillLaborOutcome {
        val message: String get() = failure.message.value
    }
}
