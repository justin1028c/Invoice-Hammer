package com.fordham.toolbelt.domain.model.agent

sealed interface ForemanRoute {
    data class LocalTab(val tab: AppTab) : ForemanRoute

    data class LocalMacro(
        val toolName: ToolName,
        val arguments: ToolArguments
    ) : ForemanRoute

    data class LlmChain(val command: NaturalLanguage) : ForemanRoute
}
