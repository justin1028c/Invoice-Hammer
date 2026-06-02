package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.cardterminal.CardBrand
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPhase
import com.fordham.toolbelt.domain.repository.CardTerminalGatewayOutcome
import com.fordham.toolbelt.domain.repository.CardTerminalPaymentGateway
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Beta simulation — does not transmit card data off-device.
 * Only last-four and brand are used for receipt labeling.
 */
class BetaCardTerminalPaymentGateway : CardTerminalPaymentGateway {

    private val _phase = MutableStateFlow(CardTerminalPhase.Idle)
    override val phase: Flow<CardTerminalPhase> = _phase.asStateFlow()

    override suspend fun process(
        lastFourDigits: String,
        brand: CardBrand,
        amountUsd: Double
    ): CardTerminalGatewayOutcome {
        if (lastFourDigits.length != 4 || amountUsd <= 0.0) {
            return CardTerminalGatewayOutcome.Failure(FailureMessage("Invalid terminal charge request."))
        }

        return try {
            _phase.value = CardTerminalPhase.SecuringConnection
            delay(PHASE_MS)
            _phase.value = CardTerminalPhase.Verifying
            delay(PHASE_MS)
            _phase.value = CardTerminalPhase.Settling
            delay(PHASE_MS)

            // Deterministic beta decline for test PAN endings in 0002
            if (lastFourDigits == "0002") {
                _phase.value = CardTerminalPhase.Failed
                CardTerminalGatewayOutcome.Failure(FailureMessage("Card declined (beta test)."))
            } else {
                _phase.value = CardTerminalPhase.Success
                CardTerminalGatewayOutcome.Success
            }
        } finally {
            if (_phase.value != CardTerminalPhase.Failed && _phase.value != CardTerminalPhase.Success) {
                _phase.value = CardTerminalPhase.Idle
            }
        }
    }

    override fun resetPhase() {
        _phase.value = CardTerminalPhase.Idle
    }

    private companion object {
        const val PHASE_MS = 900L
    }
}
