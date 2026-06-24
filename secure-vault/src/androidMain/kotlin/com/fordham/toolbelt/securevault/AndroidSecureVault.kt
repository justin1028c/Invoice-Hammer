package com.fordham.toolbelt.securevault

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

public actual class PlatformContext(public val context: Context)

internal class AndroidSecureVault(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) : SecureVaultGateway {

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

    override suspend fun storeSecret(label: SecretKeyLabel, value: SecretValue): VaultOperationResult =
        withContext(ioDispatcher) {
            try {
                val prefs = encryptedPrefs ?: return@withContext VaultOperationResult.Failure("EncryptedSharedPreferences not available.")
                prefs.edit().putString(label.value, value.value).apply()
                VaultOperationResult.Success(value)
            } catch (e: Exception) {
                VaultOperationResult.Failure(e.message ?: "Unknown Android Keystore error")
            }
        }

    override suspend fun retrieveSecret(label: SecretKeyLabel): VaultOperationResult =
        withContext(ioDispatcher) {
            try {
                val prefs = encryptedPrefs ?: return@withContext VaultOperationResult.Failure("EncryptedSharedPreferences not available.")
                val value = prefs.getString(label.value, null)
                if (value != null) {
                    VaultOperationResult.Success(SecretValue(value))
                } else {
                    VaultOperationResult.Failure("Secret not found for label: ${label.value}")
                }
            } catch (e: Exception) {
                VaultOperationResult.Failure(e.message ?: "Unknown Android Keystore error")
            }
        }
}

public actual fun createSecureVault(
    context: PlatformContext,
    ioDispatcher: CoroutineDispatcher
): SecureVaultGateway = AndroidSecureVault(context.context, ioDispatcher)
