package com.fordham.toolbelt.domain.model

sealed interface SyncOutcome {
    data object Success : SyncOutcome
    data class Failure(val error: FailureMessage) : SyncOutcome
}

sealed interface SyncUploadOutcome {
    data class Success(val path: String) : SyncUploadOutcome
    data class Failure(val error: FailureMessage) : SyncUploadOutcome
}
