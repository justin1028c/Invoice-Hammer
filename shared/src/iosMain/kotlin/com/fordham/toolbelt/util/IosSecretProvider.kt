package com.fordham.toolbelt.util

class IosSecretProvider : SecretProvider {
    override fun getGoogleClientId(): String {
        val clientId = getSecret(GOOGLE_CLIENT_ID)
        check(clientId.isNotBlank()) {
            "Google client ID is missing for iOS — set GOOGLE_CLIENT_ID in Xcode build settings / project.yml"
        }
        return clientId
    }

    override fun getSecret(key: String): String {
        return IosSecurityServiceProvider.bridge
            ?.getSecret(key)
            .orEmpty()
    }

    companion object {
        const val GOOGLE_CLIENT_ID = "google_client_id"
    }
}
