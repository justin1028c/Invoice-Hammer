package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class GenerateSummaryUseCase(
    private val geminiRepository: GeminiRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(data: String): GeminiOutcome {
        val settings = settingsRepository.businessSettingsFlow.first()
        if (!settings.isPremium) {
            return GeminiOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage("Premium Required"))
        }
        return geminiRepository.processTask(TaskType.SUMMARIZE, data)
    }
}
