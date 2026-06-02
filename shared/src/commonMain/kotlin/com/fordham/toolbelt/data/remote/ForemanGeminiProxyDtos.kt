package com.fordham.toolbelt.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ForemanGeminiProxyRequest(
    val model: String,
    val request: GeminiRequest
)
