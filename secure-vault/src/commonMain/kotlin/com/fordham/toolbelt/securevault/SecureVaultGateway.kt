package com.fordham.toolbelt.securevault

import kotlin.jvm.JvmInline

/**
 * Platform-specific context object required to initialize security and file-based services.
 * Wraps Android's Context on Android and NSObject on iOS to avoid expect/actual modality conflicts.
 */
public expect class PlatformContext

/**
 * Type-safe value class representing a unique key or label for storing secret data.
 * Eradicates primitive obsession to prevent developer error at boundary APIs.
 *
 * @property value The unique string label of the secret key.
 */
@JvmInline
public value class SecretKeyLabel(public val value: String)

/**
 * Type-safe value class representing the sensitive payload data to be stored or retrieved.
 * Eradicates primitive obsession to prevent developer error at boundary APIs.
 *
 * @property value The plain-text string representation of the secret payload.
 */
@JvmInline
public value class SecretValue(public val value: String)

/**
 * Sealed interface representing the outcome of a secure vault operation.
 * Designed without generic types for clean Swift/Objective-C interoperability.
 */
public sealed interface VaultOperationResult {
    /**
     * Represents a successful vault operation containing the retrieved or stored [SecretValue].
     *
     * @property secret The resolved secure credential.
     */
    public data class Success(public val secret: SecretValue) : VaultOperationResult

    /**
     * Represents a failed vault operation with a descriptive error reason.
     *
     * @property reason The details of the failure.
     */
    public data class Failure(public val reason: String) : VaultOperationResult
}

/**
 * Gateway contract for hardware-backed key and credential storage operations.
 * Implementations are required to be thread-safe and delegate execution off the main thread.
 */
public interface SecureVaultGateway {
    /**
     * Persists the given [value] securely under the identifier [label].
     *
     * @param label The target identifier key.
     * @param value The sensitive payload to persist.
     * @return [VaultOperationResult.Success] on successful persist, or [VaultOperationResult.Failure].
     */
    public suspend fun storeSecret(label: SecretKeyLabel, value: SecretValue): VaultOperationResult

    /**
     * Resolves the secure payload stored under the identifier [label].
     *
     * @param label The target identifier key.
     * @return [VaultOperationResult.Success] containing the secret if found, or [VaultOperationResult.Failure].
     */
    public suspend fun retrieveSecret(label: SecretKeyLabel): VaultOperationResult
}

/**
 * Factory function ensuring explicit dispatcher injection across all platform targets.
 * Ensures cryptographic and hardware operations are bounded to the injected [ioDispatcher].
 *
 * @param context The platform environment context.
 * @param ioDispatcher The coroutine dispatcher dedicated to background I/O.
 * @return A concrete platform implementation of [SecureVaultGateway].
 */
public expect fun createSecureVault(
    context: PlatformContext,
    ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
): SecureVaultGateway
