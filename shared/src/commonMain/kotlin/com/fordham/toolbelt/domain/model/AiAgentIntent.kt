package com.fordham.toolbelt.domain.model

enum class AgentMode {
    ACTION,   // Mode 1: Executing a specific app command
    RESPONSE  // Mode 2: Conversational fallback / answering questions
}

sealed class AiAgentIntent {
    data class DraftInvoice(val data: String?) : AiAgentIntent()
    data class SummarizeClient(val clientName: String) : AiAgentIntent()
    data class AnalyzeFinances(val period: String?) : AiAgentIntent()
    data class FindJob(val query: String) : AiAgentIntent()
    data class GeneralQuery(val query: String) : AiAgentIntent()
    data object ScanReceipt : AiAgentIntent()
    data object OpenStores : AiAgentIntent()
    data object PremiumRequired : AiAgentIntent()
    data object Unknown : AiAgentIntent()
}

data class AiAgentResponse(
    val mode: AgentMode,
    val summary: String,
    val actionTaken: String? = null,
    val suggestedIntent: AiAgentIntent? = null
)
