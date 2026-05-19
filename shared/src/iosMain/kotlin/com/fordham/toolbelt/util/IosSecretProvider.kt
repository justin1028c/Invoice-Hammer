package com.fordham.toolbelt.util

class IosSecretProvider : SecretProvider {
    override fun getGeminiApiKey(): String {
        val key = IosSecurityServiceProvider.bridge
            ?.getSecret(GEMINI_API_KEY)
            .orEmpty()
        check(key.isNotBlank()) { "Gemini API Key is missing for iOS" }
        return key
    }

    override fun getGeminiModelName(): String {
        return "gemini-1.5-flash"
    }

    override fun getGoogleClientId(): String {
        return "716278040823-ngqvn2n3td42nrr6nbe4e3jlki348apa.apps.googleusercontent.com"
    }

    private companion object {
        const val GEMINI_API_KEY = "gemini_api_key"
    }
}
