package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.SaveBusinessLogoOutcome
import com.fordham.toolbelt.domain.model.SettingsOutcome
import com.fordham.toolbelt.domain.model.StorageOutcome
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.repository.StorageRepository

/**
 * Persists the contractor's business logo to internal storage and [BusinessSettings].
 */
class SaveBusinessLogoUseCase(
    private val storageRepository: StorageRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(pickedUri: String?): SaveBusinessLogoOutcome {
        val current = settingsRepository.getBusinessSettings()
        if (pickedUri.isNullOrBlank()) {
            return when (settingsRepository.saveBusinessSettings(current.copy(logoUri = null))) {
                is SettingsOutcome.Success -> SaveBusinessLogoOutcome.Cleared
                is SettingsOutcome.Failure -> SaveBusinessLogoOutcome.Failure(
                    FailureMessage("Could not remove saved logo.")
                )
            }
        }

        val stablePath = when (val stored = storageRepository.saveUriToPictures(pickedUri, "LOGO")) {
            is StorageOutcome.Success -> stored.path
            is StorageOutcome.Failure -> return SaveBusinessLogoOutcome.Failure(
                FailureMessage("Could not save logo image.")
            )
        }

        return when (settingsRepository.saveBusinessSettings(current.copy(logoUri = stablePath))) {
            is SettingsOutcome.Success -> SaveBusinessLogoOutcome.Saved(stablePath)
            is SettingsOutcome.Failure -> SaveBusinessLogoOutcome.Failure(
                FailureMessage("Logo saved locally but settings update failed.")
            )
        }
    }
}
