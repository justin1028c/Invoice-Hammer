package com.fordham.toolbelt.domain.model

/** How Invoice Hammer is connected to Stellar PowerPay for this build. */
sealed interface PowerPayConnectionMode {
    data object Demo : PowerPayConnectionMode

    data class Live(
        val environmentLabel: String,
        val presetLabel: String
    ) : PowerPayConnectionMode
}
