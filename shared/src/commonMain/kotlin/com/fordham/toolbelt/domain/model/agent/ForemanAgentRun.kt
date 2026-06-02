package com.fordham.toolbelt.domain.model.agent

data class ForemanAgentRun(
    val outcome: AgentOutcome,
    val session: ForemanSession
)
