package com.fordham.toolbelt.domain.model.agent

import kotlin.jvm.JvmInline

@JvmInline
value class ParsedAmount(val value: Double) {
    init {
        require(value >= 0.0) { "Amount cannot be negative" }
    }
}

sealed interface VoiceSessionState {
    data object Idle : VoiceSessionState
    data object AwaitingClientName : VoiceSessionState
    data object AwaitingClientAddress : VoiceSessionState
    data object AwaitingItemCategory : VoiceSessionState
    data object AwaitingItemDescription : VoiceSessionState
    data object AwaitingItemAmount : VoiceSessionState
}

sealed interface VoiceStepOutcome {
    data class Transition(val nextState: VoiceSessionState, val prompt: String) : VoiceStepOutcome
    data class Complete(
        val clientName: String,
        val address: String,
        val category: String,
        val description: String,
        val amount: ParsedAmount
    ) : VoiceStepOutcome
    data class Error(val message: String, val retryPrompt: String) : VoiceStepOutcome
}
