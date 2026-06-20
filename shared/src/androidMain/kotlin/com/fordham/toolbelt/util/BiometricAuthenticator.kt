package com.fordham.toolbelt.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import javax.crypto.Cipher

class BiometricAuthenticator(private val activity: FragmentActivity) {

    private val executor: Executor = ContextCompat.getMainExecutor(activity)

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    fun promptAuthenticate(
        title: String,
        subtitle: String,
        cipher: Cipher? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder().apply {
            setTitle(title)
            setSubtitle(subtitle)
            if (cipher != null) {
                setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                setNegativeButtonText(UserFacingCopy.Common.cancel())
            } else {
                setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            }
        }.build()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val authenticatedCipher = result.cryptoObject?.cipher
                if (cipher != null && authenticatedCipher == null) {
                    onError(UserFacingCopy.Platform.cryptographicAuthFailed())
                    return
                }
                try {
                    // Hardware check: attempt to encrypt a dummy token to ensure the key is unlocked
                    authenticatedCipher?.doFinal("verify_auth".toByteArray())
                    onSuccess()
                } catch (e: Exception) {
                    AppLogger.e("BiometricAuthenticator", "Mock encryption verification failed", e)
                    onError(UserFacingCopy.Platform.biometricVerificationFailed(e.message))
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        })

        if (cipher != null) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}
