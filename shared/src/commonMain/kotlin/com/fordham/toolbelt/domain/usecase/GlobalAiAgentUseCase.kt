package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class GlobalAiAgentUseCase(
    private val geminiRepository: GeminiRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(userInput: NaturalLanguage): AgentCommandOutcome {
        val settings = settingsRepository.businessSettingsFlow.first()
        if (!settings.isPremium) {
            return AgentCommandOutcome.Failure(FailureMessage("Premium Required"))
        }
        return geminiRepository.processAgentCommand(userInput.value)
    }
}
