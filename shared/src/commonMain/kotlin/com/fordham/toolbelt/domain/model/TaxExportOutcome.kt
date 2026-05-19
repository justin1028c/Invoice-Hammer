package com.fordham.toolbelt.domain.model

sealed interface TaxExportOutcome {
    data class Success(val path: String) : TaxExportOutcome
    data class Failure(val error: FailureMessage) : TaxExportOutcome
    data object Loading : TaxExportOutcome
}
