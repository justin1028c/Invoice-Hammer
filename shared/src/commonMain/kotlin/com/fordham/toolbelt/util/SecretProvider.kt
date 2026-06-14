package com.fordham.toolbelt.util

interface SecretProvider {
    fun getGoogleClientId(): String
    fun getSecret(key: String): String
}
