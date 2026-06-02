package com.fordham.toolbelt.domain.repository

/**
 * Optional job-note enrichment for Foreman planner context (implementation uses Room).
 */
interface ForemanJobMemoryPort {
    suspend fun appendRelevantNotes(context: String, userInput: String): String
}

object NoOpForemanJobMemoryPort : ForemanJobMemoryPort {
    override suspend fun appendRelevantNotes(context: String, userInput: String): String = context
}
