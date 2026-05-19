package com.fordham.toolbelt.domain.model

sealed interface PhotoOutcome {
    data object Success : PhotoOutcome
    data class Failure(val error: FailureMessage) : PhotoOutcome
}

sealed interface PhotoListOutcome {
    data class Success(val photos: List<JobPhoto>) : PhotoListOutcome
    data class Failure(val error: FailureMessage) : PhotoListOutcome
}
