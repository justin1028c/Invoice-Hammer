package com.fordham.toolbelt.domain.model

sealed interface GeminiOutcome {
    data class Success(val text: String) : GeminiOutcome
    data class Failure(val error: FailureMessage) : GeminiOutcome
}

sealed interface AgentCommandOutcome {
    data class Success(val response: AiAgentResponse) : AgentCommandOutcome
    data class Failure(val error: FailureMessage) : AgentCommandOutcome
}

sealed interface ToolCallOutcome {
    data class Success(
        val toolCall: ForemanToolCall?,
        /** Populated when [toolCall] is null (UNKNOWN / conversational completion). */
        val completionReasoning: String = ""
    ) : ToolCallOutcome
    data class Failure(val error: FailureMessage) : ToolCallOutcome
}

sealed interface InvoiceTextOutcome {
    data class Success(val result: AiInvoiceResult) : InvoiceTextOutcome
    data class Failure(val error: FailureMessage) : InvoiceTextOutcome
}

sealed interface ReceiptImageOutcome {
    data class Success(val items: List<ReceiptItem>) : ReceiptImageOutcome
    data class Failure(val error: FailureMessage) : ReceiptImageOutcome
}
