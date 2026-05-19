package com.fordham.toolbelt.data.implementation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.SettingsOutcome
import com.fordham.toolbelt.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val businessSettingsFlow: Flow<BusinessSettings> = dataStore.data.map { prefs ->
        BusinessSettings(
            businessName = prefs[BUSINESS_NAME] ?: "",
            businessSlogan = prefs[BUSINESS_SLOGAN] ?: "",
            businessPhone = prefs[BUSINESS_PHONE] ?: "",
            businessEmail = prefs[BUSINESS_EMAIL] ?: "",
            businessAddress = prefs[BUSINESS_ADDRESS] ?: "",
            isPremium = prefs[IS_PREMIUM] ?: false,
            taxRate = prefs[TAX_RATE] ?: 0.0,
            markupPercentage = prefs[MARKUP_RATE] ?: 0.0,
            logoUri = prefs[LOGO_URI],
            isDarkMode = prefs[DARK_MODE] ?: true,
            useMetricUnits = prefs[USE_METRIC] ?: false,
            notificationsEnabled = prefs[NOTIFICATIONS] ?: true
        )
    }

    override suspend fun getBusinessSettings(): BusinessSettings = businessSettingsFlow.first()

    override suspend fun saveBusinessSettings(settings: BusinessSettings): SettingsOutcome = try {
        dataStore.edit { prefs ->
            prefs[BUSINESS_NAME] = settings.businessName
            prefs[BUSINESS_SLOGAN] = settings.businessSlogan
            prefs[BUSINESS_PHONE] = settings.businessPhone
            prefs[BUSINESS_EMAIL] = settings.businessEmail
            prefs[BUSINESS_ADDRESS] = settings.businessAddress
            prefs[IS_PREMIUM] = settings.isPremium
            prefs[TAX_RATE] = settings.taxRate
            prefs[MARKUP_RATE] = settings.markupPercentage
            prefs[DARK_MODE] = settings.isDarkMode
            prefs[USE_METRIC] = settings.useMetricUnits
            prefs[NOTIFICATIONS] = settings.notificationsEnabled
            settings.logoUri?.let { prefs[LOGO_URI] = it } ?: prefs.remove(LOGO_URI)
        }
        SettingsOutcome.Success
    } catch (e: Exception) {
        SettingsOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save settings"))
    }

    companion object {
        private val BUSINESS_NAME = stringPreferencesKey("business_name")
        private val BUSINESS_SLOGAN = stringPreferencesKey("business_slogan")
        private val BUSINESS_PHONE = stringPreferencesKey("business_phone")
        private val BUSINESS_EMAIL = stringPreferencesKey("business_email")
        private val BUSINESS_ADDRESS = stringPreferencesKey("business_address")
        private val IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val TAX_RATE = doublePreferencesKey("tax_rate")
        private val MARKUP_RATE = doublePreferencesKey("markup_rate")
        private val LOGO_URI = stringPreferencesKey("logo_uri")
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val USE_METRIC = booleanPreferencesKey("use_metric")
        private val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }
}
