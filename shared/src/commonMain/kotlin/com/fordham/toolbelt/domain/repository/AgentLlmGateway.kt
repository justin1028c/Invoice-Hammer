package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage

interface AgentLlmGateway {
    suspend fun prompt(
        systemPrompt: NaturalLanguage,
        session: ForemanSession,
        functions: List<AgentFunction>
    ): AgentOutcome
}
