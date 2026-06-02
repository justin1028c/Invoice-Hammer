package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.cardterminal.CardBrand
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPhase
import kotlinx.coroutines.flow.Flow

sealed interface CardTerminalGatewayOutcome {
    data object Success : CardTerminalGatewayOutcome
    data class Failure(val error: FailureMessage) : CardTerminalGatewayOutcome
}

interface CardTerminalPaymentGateway {
    val phase: Flow<CardTerminalPhase>
    fun resetPhase()
    suspend fun process(
        lastFourDigits: String,
        brand: CardBrand,
        amountUsd: Double
    ): CardTerminalGatewayOutcome
}
