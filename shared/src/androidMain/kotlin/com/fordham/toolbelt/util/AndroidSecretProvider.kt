package com.fordham.toolbelt.util

import android.content.Context

class AndroidSecretProvider(private val context: Context) : SecretProvider {
    override fun getGoogleClientId(): String {
        val clientId = try {
            val clazz = Class.forName("${context.packageName}.BuildConfig")
            clazz.getField("GOOGLE_CLIENT_ID").get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }
        check(clientId.isNotBlank()) {
            "GOOGLE_CLIENT_ID BuildConfig field is missing or blank. " +
                "Add buildConfigField(\"String\", \"GOOGLE_CLIENT_ID\", '\"<your-client-id>\"') to app/build.gradle.kts"
        }
        return clientId
    }
}
