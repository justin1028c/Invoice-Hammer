package com.fordham.toolbelt.data

import com.fordham.toolbelt.securevault.SecureVaultGateway
import com.fordham.toolbelt.securevault.PlatformContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal expect fun buildRoomDatabase(
    passphrase: ByteArray,
    context: PlatformContext
): AppDatabase

internal expect fun generateSecurePassphrase(): String

public class DatabaseProvider(
    private val secureVault: SecureVaultGateway,
    private val platformContext: PlatformContext,
    private val ioDispatcher: CoroutineDispatcher
) {
    private var databaseInstance: AppDatabase? = null
    private val mutex = Mutex()

    public suspend fun getDatabase(): AppDatabase {
        return databaseInstance ?: mutex.withLock {
            databaseInstance ?: initializeDatabase().also { databaseInstance = it }
        }
    }

    private suspend fun initializeDatabase(): AppDatabase {
        val label = com.fordham.toolbelt.securevault.SecretKeyLabel("db_passphrase_v1")
        val secretValue = when (val result = secureVault.retrieveSecret(label)) {
            is com.fordham.toolbelt.securevault.VaultOperationResult.Success -> result.secret.value
            is com.fordham.toolbelt.securevault.VaultOperationResult.Failure -> {
                val newPassphrase = generateSecurePassphrase()
                when (val storeResult = secureVault.storeSecret(label, com.fordham.toolbelt.securevault.SecretValue(newPassphrase))) {
                    is com.fordham.toolbelt.securevault.VaultOperationResult.Success -> newPassphrase
                    is com.fordham.toolbelt.securevault.VaultOperationResult.Failure -> throw IllegalStateException("Database key storage failed: ${storeResult.reason}")
                }
            }
        }
        
        return buildRoomDatabase(secretValue.encodeToByteArray(), platformContext)
    }
}
