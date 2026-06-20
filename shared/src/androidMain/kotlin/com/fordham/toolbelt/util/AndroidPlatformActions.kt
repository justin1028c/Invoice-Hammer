package com.fordham.toolbelt.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class AndroidPlatformActions(private val context: Context) : PlatformActions {
    init {
        AppLogger.d(TAG, "Instance created: ${this.hashCode()}")
    }
    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun shareFile(path: String, title: String) {
        shareDocument(path, title)
    }

    override fun shareDocument(
        path: String,
        title: String,
        mimeType: String,
        recipientEmail: String,
        recipientPhone: String,
        subject: String,
        body: String
    ) {
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            if (recipientEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            }
            if (subject.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
            if (body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, body)
            if (recipientPhone.isNotBlank()) {
                putExtra("address", recipientPhone)
                putExtra("sms_body", body)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    override fun shareText(
        text: String,
        subject: String,
        recipientEmail: String,
        recipientPhone: String
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (recipientEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            }
            if (subject.isNotBlank()) {
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            if (recipientPhone.isNotBlank()) {
                putExtra("address", recipientPhone)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    override fun openPdf(path: String) {
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun callPhone(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun sendEmail(email: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun requestPermission(permission: String, onGranted: () -> Unit) {
        when (permission) {
            Permission.POST_NOTIFICATIONS -> {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                    onGranted()
                    return
                }
                if (isPermissionGranted(permission)) {
                    onGranted()
                    return
                }
                notificationPermissionRequester?.invoke { granted ->
                    if (granted) onGranted()
                }
            }
            Permission.CAMERA, Permission.RECORD_AUDIO -> {
                if (isPermissionGranted(permission)) {
                    onGranted()
                } else {
                    runtimePermissionRequester?.invoke(permission, onGranted)
                }
            }
        }
    }

    override fun isPermissionGranted(permission: String): Boolean {
        val androidPermission = when (permission) {
            Permission.CAMERA -> android.Manifest.permission.CAMERA
            Permission.RECORD_AUDIO -> android.Manifest.permission.RECORD_AUDIO
            Permission.POST_NOTIFICATIONS -> {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                    return true
                }
                android.Manifest.permission.POST_NOTIFICATIONS
            }
            else -> return false
        }
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            androidPermission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun showToast(message: String) {
        Toast.makeText(context, localizePlatformToast(message), Toast.LENGTH_SHORT).show()
    }

    private fun localizePlatformToast(message: String): String {
        if (!message.startsWith(UiMessageKeys.PREFIX)) return message
        return when (message) {
            UiMessageKeys.CAMERA_UNAVAILABLE -> UserFacingCopy.Platform.cameraUnavailable()
            UiMessageKeys.CAMERA_PERMISSION_REQUIRED -> UserFacingCopy.Platform.cameraPermissionRequired()
            else -> message
        }
    }

    override fun launchApp(packageName: String, fallbackUrl: String) {
        if (packageName.isBlank()) {
            openUrl(fallbackUrl)
            return
        }
        val intent = try {
            context.packageManager.getLaunchIntentForPackage(packageName)
        } catch (e: Exception) {
            null
        }
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            openUrl(fallbackUrl)
        }
    }

    // Platform-specific hooks
    var activity: androidx.fragment.app.FragmentActivity? = null
    var googleSignInLauncher: ((onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null
    var notificationPermissionRequester: ((onResult: (Boolean) -> Unit) -> Unit)? = null
    var runtimePermissionRequester: ((permission: String, onGranted: () -> Unit) -> Unit)? = null

    override fun signInWithGoogle(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        googleSignInLauncher?.invoke(onSuccess, onError)
            ?: onError(UserFacingCopy.Platform.signInLauncherNotRegistered())
    }

    override fun signOut() {
        // Sign out logic will be handled via AuthRepository, 
        // but we can clear platform-specific cache here if needed.
    }

    override fun authenticateBiometric(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        activity?.let {
            val securityManager = runCatching {
                org.koin.core.context.GlobalContext.get().get<SecurityManager>()
            }.getOrNull()
            val cipher = try {
                securityManager?.getBiometricCipher()
            } catch (e: android.security.keystore.KeyPermanentlyInvalidatedException) {
                AppLogger.e("AndroidPlatformActions", "Biometrics changed or invalidated", e)
                onError("BIOMETRIC_LOCK_INVALIDATED")
                return
            } catch (e: Exception) {
                AppLogger.e("AndroidPlatformActions", "Failed to retrieve biometric cipher", e)
                null
            }
            val authenticator = BiometricAuthenticator(it)
            authenticator.promptAuthenticate(title, subtitle, cipher, onSuccess, onError)
        } ?: onError(UserFacingCopy.Platform.noActiveActivity())
    }

    override fun isBiometricAvailable(): Boolean {
        return activity?.let { BiometricAuthenticator(it).isBiometricAvailable() } ?: false
    }

    private var onImagePicked: ((String?) -> Unit)? = null
    var imagePickerLauncher: (() -> Unit)? = null
    var cameraLauncher: (() -> Unit)? = null
    /** Launches Google account consent UI when Drive scope needs user approval. */
    var driveAuthRecoveryLauncher: ((android.content.Intent, () -> Unit) -> Unit)? = null

    fun handleImageResult(uri: String?) {
        onImagePicked?.invoke(uri)
    }

    override fun pickImage(onResult: (String?) -> Unit) {
        AppLogger.d(TAG, "pickImage called")
        onImagePicked = onResult
        if (imagePickerLauncher == null) {
            AppLogger.e(TAG, "imagePickerLauncher is NULL")
        }
        imagePickerLauncher?.invoke() ?: onResult(null)
    }

    override fun capturePhoto(onResult: (String?) -> Unit) {
        onImagePicked = onResult
        val launch = {
            val launcher = cameraLauncher
            if (launcher == null) {
                showToast(UiMessageKeys.CAMERA_UNAVAILABLE)
                onResult(null)
            } else {
                launcher.invoke()
            }
        }
        if (isPermissionGranted(Permission.CAMERA)) {
            launch()
        } else {
            requestPermission(Permission.CAMERA) {
                if (isPermissionGranted(Permission.CAMERA)) {
                    launch()
                } else {
                    showToast(UiMessageKeys.CAMERA_PERMISSION_REQUIRED)
                    onResult(null)
                }
            }
        }
    }

    override fun scheduleNotification(id: String, title: String, body: String, delayMillis: Long) {
        if (id != UnpaidInvoiceReminders.WORK_ID) return

        val constraints = androidx.work.Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.fordham.toolbelt.worker.UnpaidInvoiceWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        )
            .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        val workManager = androidx.work.WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(
            id,
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        // Run once immediately so the user gets feedback without waiting for initialDelay.
        val immediateCheck = androidx.work.OneTimeWorkRequestBuilder<com.fordham.toolbelt.worker.UnpaidInvoiceWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(
            "${id}_immediate",
            androidx.work.ExistingWorkPolicy.REPLACE,
            immediateCheck
        )
    }

    override fun cancelScheduledNotification(id: String) {
        val workManager = androidx.work.WorkManager.getInstance(context)
        workManager.cancelUniqueWork(id)
        workManager.cancelUniqueWork("${id}_immediate")
    }

    override fun triggerBackgroundSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.fordham.toolbelt.worker.SyncQueueWorker>()
            .setConstraints(constraints)
            .build()
        androidx.work.WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "sync_queue_backup",
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }
}

private const val TAG = "PlatformActions"
