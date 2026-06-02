package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.agent.ForemanSession

data class PersistedForemanState(
    val session: ForemanSession,
    val lastSystemPrompt: String
)

interface ForemanSessionPersistencePort {
    suspend fun load(): PersistedForemanState?
    suspend fun save(state: PersistedForemanState)
    suspend fun clear()
}

object NoOpForemanSessionPersistencePort : ForemanSessionPersistencePort {
    override suspend fun load(): PersistedForemanState? = null
    override suspend fun save(state: PersistedForemanState) = Unit
    override suspend fun clear() = Unit
}
