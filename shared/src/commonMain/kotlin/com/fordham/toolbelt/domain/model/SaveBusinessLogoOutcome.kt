package com.fordham.toolbelt.domain.model

sealed interface SaveBusinessLogoOutcome {
    data class Saved(val stablePath: String) : SaveBusinessLogoOutcome
    data object Cleared : SaveBusinessLogoOutcome
    data class Failure(val error: FailureMessage) : SaveBusinessLogoOutcome
}
