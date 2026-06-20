package com.fordham.toolbelt.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AndroidSecretProvider(private val context: Context) : SecretProvider {

    private val encryptedPrefs by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "secure_vault",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun getGoogleClientId(): String {
        val clientId = getSecret("GOOGLE_CLIENT_ID")
        check(clientId.isNotBlank()) {
            "GOOGLE_CLIENT_ID BuildConfig field is missing or blank. " +
                "Add buildConfigField(\"String\", \"GOOGLE_CLIENT_ID\", '\"<your-client-id>\"') to app/build.gradle.kts"
        }
        return clientId
    }

    override fun getSecret(key: String): String {
        val securedValue = try {
            encryptedPrefs?.getString(key, null)
        } catch (_: Exception) {
            null
        }
        if (!securedValue.isNullOrBlank()) {
            return securedValue
        }

        val fallback = try {
            val clazz = Class.forName("${context.packageName}.BuildConfig")
            val buildConfigKey = resolveBuildConfigKey(key)
            clazz.getField(buildConfigKey).get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }
        return fallback
    }

    private fun resolveBuildConfigKey(key: String): String {
        if (key == "GOOGLE_CLIENT_ID") return "GOOGLE_CLIENT_ID"
        return key.uppercase().replace('.', '_')
    }
}
