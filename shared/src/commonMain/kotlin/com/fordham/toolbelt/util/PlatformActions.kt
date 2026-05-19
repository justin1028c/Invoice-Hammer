package com.fordham.toolbelt.util

interface PlatformActions {
    fun openUrl(url: String)
    fun shareFile(path: String, title: String)
    fun openPdf(path: String)
    fun callPhone(phoneNumber: String)
    fun sendEmail(email: String)
    fun requestPermission(permission: String, onGranted: () -> Unit)
    fun isPermissionGranted(permission: String): Boolean
    fun showToast(message: String)
    fun launchApp(packageName: String, fallbackUrl: String)
    fun signInWithGoogle(onSuccess: (String) -> Unit, onError: (String) -> Unit)
    fun signOut()
    fun authenticateBiometric(title: String, subtitle: String, onSuccess: () -> Unit, onError: (String) -> Unit)
    fun isBiometricAvailable(): Boolean
    fun pickImage(onResult: (String?) -> Unit)
    fun capturePhoto(onResult: (String?) -> Unit)
    fun scheduleNotification(id: String, title: String, body: String, delayMillis: Long)
}

object Permission {
    const val RECORD_AUDIO = "record_audio"
    const val CAMERA = "camera"
}
