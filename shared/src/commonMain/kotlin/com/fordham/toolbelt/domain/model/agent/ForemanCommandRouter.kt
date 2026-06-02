package com.fordham.toolbelt.domain.model.agent

/**
 * Single entry for local vs LLM routing. Tab nav and high-confidence macros skip Gemini.
 */
object ForemanCommandRouter {
    fun route(rawCommand: String): ForemanRoute {
        val normalized = ForemanSttNormalizer.normalize(rawCommand)
        if (normalized.isBlank()) {
            return ForemanRoute.LlmChain(NaturalLanguage(rawCommand.trim()))
        }

        ForemanTabNavigation.parseNavigationOnly(normalized)?.let { tab ->
            return ForemanRoute.LocalTab(tab)
        }

        ForemanLocalMacroParser.parse(normalized)?.let { macro ->
            return macro
        }

        return ForemanRoute.LlmChain(NaturalLanguage(normalized))
    }
}
