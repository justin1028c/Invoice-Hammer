package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.repository.ForemanSessionPersistencePort
import com.fordham.toolbelt.domain.repository.PersistedForemanState
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Foreman session state with background DataStore persistence.
 */
class ForemanSessionStore(
    private val persistence: ForemanSessionPersistencePort,
    private val scope: CoroutineScope,
    initialSessionId: SessionId = SessionId(randomUUID())
) {
    var session: ForemanSession = ForemanSession.empty(initialSessionId)
        private set
    var completedSteps: MutableList<ChainedToolStep> = mutableListOf()
        private set
    var lastSystemPrompt: String = ""
        private set

    @Volatile
    private var restored = false

    suspend fun ensureRestored() {
        if (restored) return
        persistence.load()?.let { state ->
            session = state.session
            lastSystemPrompt = state.lastSystemPrompt
            completedSteps.clear()
        }
        restored = true
    }

    fun setSystemPrompt(prompt: String) {
        lastSystemPrompt = prompt
        persistAsync()
    }

    fun updateSession(updated: ForemanSession) {
        session = updated
        persistAsync()
    }

    fun replaceSteps(steps: List<ChainedToolStep>) {
        completedSteps = steps.toMutableList()
    }

    fun addStep(step: ChainedToolStep) {
        completedSteps.add(step)
    }

    fun clearSteps() {
        completedSteps.clear()
    }

    fun reset(sessionId: SessionId = SessionId(randomUUID())) {
        session = ForemanSession.empty(sessionId)
        completedSteps.clear()
        lastSystemPrompt = ""
        scope.launch { persistence.clear() }
    }

    private fun persistAsync() {
        scope.launch {
            if (!restored) {
                persistence.load()
                restored = true
            }
            persistence.save(
                PersistedForemanState(
                    session = session,
                    lastSystemPrompt = lastSystemPrompt
                )
            )
        }
    }
}
