package com.fordham.toolbelt.domain.model.agent

data class ChainedToolStep(
    val toolCallId: ToolCallId,
    val toolName: ToolName,
    val result: ToolExecutionResult
)
