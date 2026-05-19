package com.fordham.toolbelt.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class AndroidPlatformActions(private val context: Context) : PlatformActions {
    init {
        println("PLATFORM_ACTIONS: Instance created: ${this.hashCode()}")
    }
    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun shareFile(path: String, title: String) {
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
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
        // This usually needs to be handled in the Activity, 
        // but for now we'll provide the logic to check/ask.
        // In a real KMP app, we might use a library like MOKO Permissions.
    }

    override fun isPermissionGranted(permission: String): Boolean {
        val androidPermission = when(permission) {
            Permission.CAMERA -> android.Manifest.permission.CAMERA
            Permission.RECORD_AUDIO -> android.Manifest.permission.RECORD_AUDIO
            else -> return false
        }
        return androidx.core.content.ContextCompat.checkSelfPermission(context, androidPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun launchApp(packageName: String, fallbackUrl: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
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

    override fun signInWithGoogle(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        googleSignInLauncher?.invoke(onSuccess, onError) ?: onError("Sign-in launcher not registered")
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
            val authenticator = BiometricAuthenticator(it)
            authenticator.promptAuthenticate(title, subtitle, null, onSuccess, onError)
        } ?: onError("No active activity context")
    }

    override fun isBiometricAvailable(): Boolean {
        return activity?.let { BiometricAuthenticator(it).isBiometricAvailable() } ?: false
    }

    private var onImagePicked: ((String?) -> Unit)? = null
    var imagePickerLauncher: (() -> Unit)? = null
    var cameraLauncher: (() -> Unit)? = null

    fun handleImageResult(uri: String?) {
        onImagePicked?.invoke(uri)
    }

    override fun pickImage(onResult: (String?) -> Unit) {
        println("PLATFORM_ACTIONS: pickImage called")
        onImagePicked = onResult
        if (imagePickerLauncher == null) {
            println("PLATFORM_ACTIONS: imagePickerLauncher is NULL")
        }
        imagePickerLauncher?.invoke() ?: onResult(null)
    }

    override fun capturePhoto(onResult: (String?) -> Unit) {
        println("PLATFORM_ACTIONS: capturePhoto called")
        onImagePicked = onResult
        if (cameraLauncher == null) {
            println("PLATFORM_ACTIONS: cameraLauncher is NULL")
        }
        cameraLauncher?.invoke() ?: onResult(null)
    }

    override fun scheduleNotification(id: String, title: String, body: String, delayMillis: Long) {
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.fordham.toolbelt.worker.UnpaidInvoiceWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        ).setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            id,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
