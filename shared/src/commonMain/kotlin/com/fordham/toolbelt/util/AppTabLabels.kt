package com.fordham.toolbelt.util

import com.fordham.toolbelt.domain.model.agent.AppTab

/**
 * Locale-aware tab labels for Foreman messages (matches Compose tab strings).
 */
object AppTabLabels {
    private val spanishByNavLabel = mapOf(
        "NEW" to "NUEVO",
        "PAST" to "HISTORIAL",
        "RECEIPTS" to "RECIBOS",
        "STATS" to "ESTADÍSTICAS",
        "STORES" to "TIENDAS",
        "CLIENTS" to "CLIENTES",
        "SETTINGS" to "AJUSTES"
    )

    fun localizedNavLabel(englishNavLabel: String): String {
        if (AppLocale.fromSystem() != AppLocale.Spanish) return englishNavLabel
        return spanishByNavLabel[englishNavLabel] ?: englishNavLabel
    }

    fun localizedNavLabel(tab: AppTab): String = localizedNavLabel(tab.navLabel)

    fun openedTabMessage(tab: AppTab): String {
        val label = localizedNavLabel(tab)
        return if (AppLocale.fromSystem() == AppLocale.Spanish) {
            "Se abrió $label."
        } else {
            "Opened $label."
        }
    }

    fun openedTabStepLabel(tab: AppTab): String {
        val label = localizedNavLabel(tab)
        return if (AppLocale.fromSystem() == AppLocale.Spanish) {
            "Se abrió $label"
        } else {
            "Opened $label"
        }
    }
}
