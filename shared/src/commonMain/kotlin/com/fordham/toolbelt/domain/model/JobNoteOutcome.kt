package com.fordham.toolbelt.domain.model

sealed interface JobNoteOutcome {
    data object Success : JobNoteOutcome
    data class Failure(val error: FailureMessage) : JobNoteOutcome
}
