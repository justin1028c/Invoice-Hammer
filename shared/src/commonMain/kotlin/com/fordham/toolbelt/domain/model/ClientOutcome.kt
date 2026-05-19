package com.fordham.toolbelt.domain.model

sealed interface ClientOutcome {
    data object Success : ClientOutcome
    data class Failure(val error: FailureMessage) : ClientOutcome
}

sealed interface ClientListOutcome {
    data class Success(val clients: List<Client>) : ClientListOutcome
    data class Failure(val error: FailureMessage) : ClientListOutcome
}
