package com.fordham.toolbelt.securevault

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeSecureVault : SecureVaultGateway {
    private val store = mutableMapOf<String, String>()

    override suspend fun storeSecret(label: SecretKeyLabel, value: SecretValue): VaultOperationResult {
        store[label.value] = value.value
        return VaultOperationResult.Success(value)
    }

    override suspend fun retrieveSecret(label: SecretKeyLabel): VaultOperationResult {
        val raw = store[label.value]
        return if (raw != null) {
            VaultOperationResult.Success(SecretValue(raw))
        } else {
            VaultOperationResult.Failure("Secret not found for key: ${label.value}")
        }
    }
}

class SecureVaultTest {

    @Test
    fun testStoreAndRetrieveSecretSuccess() = kotlinx.coroutines.test.runTest {
        val vault = FakeSecureVault()
        val label = SecretKeyLabel("test_key")
        val secretValue = SecretValue("super_secure_password_123")

        // Store
        val storeResult = vault.storeSecret(label, secretValue)
        assertTrue(storeResult is VaultOperationResult.Success)
        assertEquals(secretValue, storeResult.secret)

        // Retrieve
        val retrieveResult = vault.retrieveSecret(label)
        assertTrue(retrieveResult is VaultOperationResult.Success)
        assertEquals(secretValue, retrieveResult.secret)
    }

    @Test
    fun testRetrieveSecretNotFound() = kotlinx.coroutines.test.runTest {
        val vault = FakeSecureVault()
        val label = SecretKeyLabel("missing_key")

        val retrieveResult = vault.retrieveSecret(label)
        assertTrue(retrieveResult is VaultOperationResult.Failure)
        assertEquals("Secret not found for key: missing_key", retrieveResult.reason)
    }

    @Test
    fun testOverwritesSecret() = kotlinx.coroutines.test.runTest {
        val vault = FakeSecureVault()
        val label = SecretKeyLabel("config_key")
        val firstValue = SecretValue("value_one")
        val secondValue = SecretValue("value_two")

        vault.storeSecret(label, firstValue)
        vault.storeSecret(label, secondValue)
        val result = vault.retrieveSecret(label)
        assertTrue(result is VaultOperationResult.Success)
        assertEquals(secondValue, result.secret)
    }
}
