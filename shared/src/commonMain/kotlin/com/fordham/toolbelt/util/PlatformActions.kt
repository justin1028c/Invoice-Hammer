package com.fordham.toolbelt.util

interface PlatformActions {
    fun openUrl(url: String)
    fun shareFile(path: String, title: String)
    /** Share a document with optional email/SMS targeting (opens system chooser). */
    fun shareDocument(
        path: String,
        title: String,
        mimeType: String = "application/pdf",
        recipientEmail: String = "",
        recipientPhone: String = "",
        subject: String = "",
        body: String = ""
    )
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
    fun cancelScheduledNotification(id: String)
}

object Permission {
    const val RECORD_AUDIO = "record_audio"
    const val CAMERA = "camera"
    const val POST_NOTIFICATIONS = "post_notifications"
}
