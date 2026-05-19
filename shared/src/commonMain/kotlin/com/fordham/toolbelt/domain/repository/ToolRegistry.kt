package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.ToolArguments
import com.fordham.toolbelt.domain.model.agent.ToolExecutionResult
import com.fordham.toolbelt.domain.model.agent.ToolName

interface ToolRegistry {
    fun availableFunctions(): List<AgentFunction>

    suspend fun execute(
        toolName: ToolName,
        arguments: ToolArguments
    ): ToolExecutionResult
}
