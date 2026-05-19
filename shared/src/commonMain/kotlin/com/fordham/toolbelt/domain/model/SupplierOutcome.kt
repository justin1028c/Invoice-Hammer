package com.fordham.toolbelt.domain.model

sealed interface SupplierOutcome {
    data object Success : SupplierOutcome
    data class Failure(val error: FailureMessage) : SupplierOutcome
}

sealed interface SupplierListOutcome {
    data class Success(val suppliers: List<Supplier>) : SupplierListOutcome
    data class Failure(val error: FailureMessage) : SupplierListOutcome
}
