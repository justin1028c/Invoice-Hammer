package com.fordham.toolbelt.domain.model.agent

data class ForemanAppContextBundle(
    val systemPrompt: String,
    val runtime: ForemanRuntimeSnapshot
)
