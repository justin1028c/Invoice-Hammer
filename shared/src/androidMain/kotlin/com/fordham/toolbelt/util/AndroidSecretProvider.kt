package com.fordham.toolbelt.util

import android.content.Context

class AndroidSecretProvider(private val context: Context) : SecretProvider {
    override fun getGeminiApiKey(): String {
        val key = NativeSecrets.getGeminiKey()
        check(key.isNotBlank()) { "Gemini API Key is missing from NativeSecrets" }
        return key
    }

    override fun getGeminiModelName(): String {
        // Model name can be managed here or fetched via remote config
        return "gemini-1.5-flash"
    }

    override fun getGoogleClientId(): String {
        return "716278040823-ngqvn2n3td42nrr6nbe4e3jlki348apa.apps.googleusercontent.com"
    }
}
