package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.SettingsDataStore
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.SettingsOutcome
import com.fordham.toolbelt.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SettingsRepositoryImpl(
    private val dataStore: SettingsDataStore
) : SettingsRepository {
    override val businessSettingsFlow: Flow<BusinessSettings> = dataStore.businessSettingsFlow

    override suspend fun getBusinessSettings(): BusinessSettings = dataStore.businessSettingsFlow.first()

    override suspend fun saveBusinessSettings(settings: BusinessSettings): SettingsOutcome = try {
        dataStore.saveBusinessSettings(settings)
        SettingsOutcome.Success
    } catch (e: Exception) {
        SettingsOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save settings"))
    }
}
