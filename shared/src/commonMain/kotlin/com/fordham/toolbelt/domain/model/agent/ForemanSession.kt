package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.InvoiceId

sealed interface AgentRole {
    data object User : AgentRole
    data object Foreman : AgentRole
    data object ToolSystem : AgentRole
}

data class ForemanTurn(
    val role: AgentRole,
    val content: NaturalLanguage,
    val timestamp: TimestampMillis,
    val toolCallId: ToolCallId? = null,
    val toolName: ToolName? = null
)

data class ForemanSession(
    val sessionId: SessionId,
    val history: List<ForemanTurn>,
    val activeClient: ClientId?,
    val activeDraftInvoice: InvoiceId?,
    val resolvedEntities: ResolvedEntities
) {
    fun append(turn: ForemanTurn): ForemanSession {
        return copy(history = history + turn)
    }

    companion object {
        fun empty(sessionId: SessionId): ForemanSession {
            return ForemanSession(
                sessionId = sessionId,
                history = emptyList(),
                activeClient = null,
                activeDraftInvoice = null,
                resolvedEntities = ResolvedEntities.empty()
            )
        }
    }
}
