package com.fordham.toolbelt.util

interface SecretProvider {
    fun getGeminiApiKey(): String
    fun getGeminiModelName(): String
    fun getGoogleClientId(): String
}
