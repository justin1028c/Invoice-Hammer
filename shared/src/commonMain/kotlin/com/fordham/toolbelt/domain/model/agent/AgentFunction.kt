package com.fordham.toolbelt.domain.model.agent

sealed interface ParameterType {
    data object Text : ParameterType
    data object Integer : ParameterType
    data object Boolean : ParameterType
}

data class FunctionParameter(
    val name: ParameterName,
    val type: ParameterType,
    val required: Boolean
)

data class AgentFunction(
    val toolName: ToolName,
    val description: ToolDescription,
    val parameters: List<FunctionParameter>
)
