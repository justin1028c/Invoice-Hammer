package com.fordham.toolbelt.domain.model

sealed interface StorageOutcome {
    data class Success(val path: String) : StorageOutcome
    data class Failure(val error: FailureMessage) : StorageOutcome
}

sealed interface StorageBytesOutcome {
    data class Success(val bytes: ByteArray) : StorageBytesOutcome
    data class Failure(val error: FailureMessage) : StorageBytesOutcome
}
