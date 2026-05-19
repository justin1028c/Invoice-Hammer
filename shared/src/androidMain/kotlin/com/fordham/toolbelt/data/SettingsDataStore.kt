package com.fordham.toolbelt.data

import android.content.SharedPreferences
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.util.SecurityManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SettingsDataStore(
    private val securityManager: SecurityManager
) {
    private val encryptedPrefs = securityManager.getEncryptedPrefs()

    val businessSettingsFlow: Flow<BusinessSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(getSettings())
        }
        encryptedPrefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getSettings())
        awaitClose { encryptedPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun getSettings(): BusinessSettings {
        return BusinessSettings(
            businessName = encryptedPrefs.getString(BUSINESS_NAME, "") ?: "",
            businessSlogan = encryptedPrefs.getString(BUSINESS_SLOGAN, "") ?: "",
            businessPhone = encryptedPrefs.getString(BUSINESS_PHONE, "") ?: "",
            businessEmail = encryptedPrefs.getString(BUSINESS_EMAIL, "") ?: "",
            businessAddress = encryptedPrefs.getString(BUSINESS_ADDRESS, "") ?: "",
            isPremium = encryptedPrefs.getBoolean(IS_PREMIUM, false),
            taxRate = encryptedPrefs.getFloat(TAX_RATE, 0.0f).toDouble(),
            markupPercentage = encryptedPrefs.getFloat(MARKUP_RATE, 0.0f).toDouble(),
            logoUri = encryptedPrefs.getString(LOGO_URI, null),
            isDarkMode = encryptedPrefs.getBoolean(DARK_MODE, true),
            useMetricUnits = encryptedPrefs.getBoolean(USE_METRIC, false),
            notificationsEnabled = encryptedPrefs.getBoolean(NOTIFICATIONS, true)
        )
    }

    suspend fun saveBusinessSettings(settings: BusinessSettings) {
        encryptedPrefs.edit().apply {
            putString(BUSINESS_NAME, settings.businessName)
            putString(BUSINESS_SLOGAN, settings.businessSlogan)
            putString(BUSINESS_PHONE, settings.businessPhone)
            putString(BUSINESS_EMAIL, settings.businessEmail)
            putString(BUSINESS_ADDRESS, settings.businessAddress)
            putBoolean(IS_PREMIUM, settings.isPremium)
            putFloat(TAX_RATE, settings.taxRate.toFloat())
            putFloat(MARKUP_RATE, settings.markupPercentage.toFloat())
            putBoolean(DARK_MODE, settings.isDarkMode)
            putBoolean(USE_METRIC, settings.useMetricUnits)
            putBoolean(NOTIFICATIONS, settings.notificationsEnabled)
            
            if (settings.logoUri != null) {
                putString(LOGO_URI, settings.logoUri)
            } else {
                remove(LOGO_URI)
            }
        }.apply()
    }

    companion object {
        private const val BUSINESS_NAME = "business_name"
        private const val BUSINESS_SLOGAN = "business_slogan"
        private const val BUSINESS_PHONE = "business_phone"
        private const val BUSINESS_EMAIL = "business_email"
        private const val BUSINESS_ADDRESS = "business_address"
        private const val IS_PREMIUM = "is_premium"
        private const val TAX_RATE = "tax_rate"
        private const val MARKUP_RATE = "markup_rate"
        private const val LOGO_URI = "logo_uri"
        private const val DARK_MODE = "dark_mode"
        private const val USE_METRIC = "use_metric"
        private const val NOTIFICATIONS = "notifications_enabled"
    }
}
