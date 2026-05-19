package com.fordham.toolbelt.domain.model

sealed interface SettingsOutcome {
    data object Success : SettingsOutcome
    data class Failure(val error: FailureMessage) : SettingsOutcome
}
