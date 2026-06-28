package com.fordham.toolbelt.securevault

import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.CFTypeRefVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.kCFBooleanTrue
import platform.darwin.NSObject
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecAttrAccount
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.errSecItemNotFound

public actual class PlatformContext(public val nsObject: NSObject)

internal class IosSecureVault(
    private val ioDispatcher: CoroutineDispatcher
) : SecureVaultGateway {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun storeSecret(label: SecretKeyLabel, value: SecretValue): VaultOperationResult =
        withContext(ioDispatcher) {
            try {
                val query = NSMutableDictionary().apply {
                    setObject(kSecClassGenericPassword, kSecClass as String)
                    setObject(label.value, kSecAttrAccount as String)
                }

                // Delete any existing item first
                SecItemDelete(query as CFDictionaryRef)

                // Add new item
                val nsValue = value.value as NSString
                val data = nsValue.dataUsingEncoding(NSUTF8StringEncoding)
                query.setObject(data!!, kSecValueData as String)

                val status = SecItemAdd(query as CFDictionaryRef, null)
                if (status == errSecSuccess) {
                    VaultOperationResult.Success(value)
                } else {
                    VaultOperationResult.Failure("Keychain store error code: $status")
                }
            } catch (e: Exception) {
                VaultOperationResult.Failure(e.message ?: "Unknown iOS Keychain write error")
            }
        }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun retrieveSecret(label: SecretKeyLabel): VaultOperationResult =
        withContext(ioDispatcher) {
            try {
                val query = NSMutableDictionary().apply {
                    setObject(kSecClassGenericPassword, kSecClass as String)
                    setObject(label.value, kSecAttrAccount as String)
                    setObject(kCFBooleanTrue, kSecReturnData as String)
                    setObject(kSecMatchLimitOne, kSecMatchLimit as String)
                }

                memScoped {
                    val resultVar = alloc<CFTypeRefVar>()
                    val status = SecItemCopyMatching(query as CFDictionaryRef, resultVar.ptr)

                    when (status) {
                        errSecSuccess -> {
                            val data = CFBridgingRelease(resultVar.value) as? NSData
                            if (data != null) {
                                val decryptedString = NSString.create(data = data, encoding = NSUTF8StringEncoding) as String
                                VaultOperationResult.Success(SecretValue(decryptedString))
                            } else {
                                VaultOperationResult.Failure("Failed to cast keychain result to NSData")
                            }
                        }
                        errSecItemNotFound -> {
                            VaultOperationResult.Failure("Secret not found for label: ${label.value}")
                        }
                        else -> {
                            VaultOperationResult.Failure("Keychain retrieve error code: $status")
                        }
                    }
                }
            } catch (e: Exception) {
                VaultOperationResult.Failure(e.message ?: "Unknown iOS Keychain read error")
            }
        }
}

public actual fun createSecureVault(
    context: PlatformContext,
    ioDispatcher: CoroutineDispatcher
): SecureVaultGateway = IosSecureVault(ioDispatcher)
