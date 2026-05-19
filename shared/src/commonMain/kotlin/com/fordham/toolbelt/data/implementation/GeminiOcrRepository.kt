package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.GeminiOutcome
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.repository.OcrRepository

class GeminiOcrRepository(
    private val geminiRepository: GeminiRepository
) : OcrRepository {
    override suspend fun recognizeText(imageBytes: ByteArray): GeminiOutcome {
        return geminiRepository.processOcrImage(imageBytes)
    }
}
