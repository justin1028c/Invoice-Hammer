package com.fordham.toolbelt.domain.model.agent

object ForemanChainLimits {
    const val MAX_SAFE_STEPS_PER_COMMAND: Int = 12
    const val MAX_HISTORY_TURNS_BEFORE_COMPRESS: Int = 14
    const val MAX_TOOL_RETRIES_PER_TOOL: Int = 1
}
