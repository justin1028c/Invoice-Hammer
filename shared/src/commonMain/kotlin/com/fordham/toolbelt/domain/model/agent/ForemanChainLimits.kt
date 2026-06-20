package com.fordham.toolbelt.domain.model.agent

object ForemanChainLimits {
    const val MAX_SAFE_STEPS_PER_COMMAND: Int = 5
    const val MAX_HISTORY_TURNS_BEFORE_COMPRESS: Int = 6
    const val MAX_TOOL_RETRIES_PER_TOOL: Int = 1
}
