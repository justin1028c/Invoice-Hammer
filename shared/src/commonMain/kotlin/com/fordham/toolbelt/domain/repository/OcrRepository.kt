package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.GeminiOutcome

interface OcrRepository {
    suspend fun recognizeText(imageBytes: ByteArray): GeminiOutcome
}
