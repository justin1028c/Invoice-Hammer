package com.fordham.toolbelt.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val tools: List<GeminiTools>? = null,
    val toolConfig: GeminiToolConfig? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String = "user"
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null,
    val functionCall: GeminiFunctionCall? = null
)

@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String // Base64
)

@Serializable
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val maxOutputTokens: Int? = null,
    val temperature: Float? = null,
    val responseSchema: GeminiSchema? = null
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent
)

@Serializable
data class AiReceiptItemDto(
    val description: String,
    val totalPrice: Double
)

@Serializable
data class AiReceiptResponse(
    val items: List<AiReceiptItemDto>
)
