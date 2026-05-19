package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.SettingsOutcome
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val businessSettingsFlow: Flow<BusinessSettings>
    suspend fun getBusinessSettings(): BusinessSettings
    suspend fun saveBusinessSettings(settings: BusinessSettings): SettingsOutcome
}
