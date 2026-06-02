package com.fordham.toolbelt.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ForemanSessionPersistenceDto(
    val sessionId: String,
    val history: List<ForemanTurnDto> = emptyList(),
    val activeClientId: String? = null,
    val activeDraftInvoiceId: String? = null,
    val resolvedEntities: List<ResolvedEntityEntryDto> = emptyList(),
    val lastSystemPrompt: String = ""
)

@Serializable
data class ForemanTurnDto(
    val role: String,
    val content: String,
    val timestamp: Long,
    val toolCallId: String? = null,
    val toolName: String? = null
)

@Serializable
data class ResolvedEntityEntryDto(
    val alias: String,
    val entityType: String,
    val entityId: String
)
