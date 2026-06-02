package com.fordham.toolbelt

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.net.Uri
import com.fordham.toolbelt.domain.model.agent.AppTab
import com.fordham.toolbelt.navigation.MainTabNavigation
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.fordham.toolbelt.billing.BillingActivityHolder
import com.fordham.toolbelt.data.remote.StripeConfig
import com.fordham.toolbelt.stripe.StripePaymentSheetCoordinator
import com.fordham.toolbelt.util.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import org.koin.android.ext.android.inject
import java.io.File
import com.fordham.toolbelt.domain.deeplink.DeepLinkDispatcher

class MainActivity : FragmentActivity() {

    private fun googleSignInErrorMessage(e: ApiException): String = when (e.statusCode) {
        CommonStatusCodes.DEVELOPER_ERROR, 10 -> "Google Sign-In is not configured for this APK. " +
            "Add SHA-1 5c64756a1cd541582faaf8ddb20481eeb4446e08 to Firebase, download new google-services.json, rebuild. " +
            "See FIREBASE_GOOGLE_SIGNIN.md."
        CommonStatusCodes.CANCELED, 12501 -> "Sign-in cancelled. Tap SIGN IN again and choose your Google account."
        CommonStatusCodes.NETWORK_ERROR, 7 -> "Network error during sign-in. Check connection and try again."
        else -> e.message?.let { "$it (code ${e.statusCode})" } ?: "Sign-in failed (code ${e.statusCode})"
    }
    
    private val platformActions: PlatformActions by inject()
    private val stripeConfig: StripeConfig by inject()
    private val deepLinkDispatcher: DeepLinkDispatcher by inject()

    private var onGoogleSignInSuccess: ((String) -> Unit)? = null
    private var onGoogleSignInError: ((String) -> Unit)? = null
    private var onNotificationPermissionResult: ((Boolean) -> Unit)? = null
    private var onRuntimePermissionGranted: (() -> Unit)? = null

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Always parse the intent — config errors often return RESULT_CANCELED with ApiException in the task.
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                onGoogleSignInSuccess?.invoke(idToken)
            } else {
                onGoogleSignInError?.invoke(
                    "Google did not return an ID token. Add this APK's SHA-1 in Firebase (see FIREBASE_GOOGLE_SIGNIN.md)."
                )
            }
        } catch (e: ApiException) {
            onGoogleSignInError?.invoke(googleSignInErrorMessage(e))
        } catch (e: Exception) {
            onGoogleSignInError?.invoke(e.message ?: "Sign-in failed")
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        (platformActions as? AndroidPlatformActions)?.handleImageResult(uri?.toString())
    }

    private var photoUri: Uri? = null
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onNotificationPermissionResult?.invoke(granted)
        onNotificationPermissionResult = null
    }

    private val runtimePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Always notify — callers re-check isPermissionGranted (handles deny without hanging).
        onRuntimePermissionGranted?.invoke()
        onRuntimePermissionGranted = null
    }

    private var onDriveAuthRecoveryComplete: (() -> Unit)? = null

    private val driveAuthRecoveryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        onDriveAuthRecoveryComplete?.invoke()
        onDriveAuthRecoveryComplete = null
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            (platformActions as? AndroidPlatformActions)?.handleImageResult(photoUri?.toString())
        } else {
            (platformActions as? AndroidPlatformActions)?.handleImageResult(null)
        }
    }

    fun launchImagePicker() {
        println("MAIN_ACTIVITY: launchImagePicker called")
        imagePickerLauncher.launch("image/*")
    }

    fun launchCamera() {
        try {
            // cache-path in file_paths.xml — camera app writes via FileProvider grant
            val photosDir = File(cacheDir, "photos").apply { mkdirs() }
            val photoFile = File(photosDir, "captured_photo_${System.currentTimeMillis()}.jpg")
            photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)
            cameraLauncher.launch(photoUri!!)
        } catch (e: Exception) {
            photoUri = null
            (platformActions as? AndroidPlatformActions)?.handleImageResult(null)
            android.widget.Toast.makeText(
                this,
                "Could not open camera: ${e.message ?: "unknown error"}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BillingActivityHolder.activity = this
        if (stripeConfig.isPublishableKeyConfigured) {
            StripePaymentSheetCoordinator.register(this, stripeConfig.publishableKey)
        }
        val launchTab = consumeNavigationTab(intent)
        intent?.data?.toString()?.let { uriString ->
            deepLinkDispatcher.dispatch(uriString)
        }
        
        // Register platform hooks
        (platformActions as? AndroidPlatformActions)?.let { actions ->
            actions.activity = this
            actions.googleSignInLauncher = { success, error ->
                onGoogleSignInSuccess = success
                onGoogleSignInError = error
                val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInConfig.options(this))
                // Fresh account picker; avoids stale session returning instant "cancelled".
                googleSignInClient.signOut().addOnCompleteListener {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                }
            }
            actions.driveAuthRecoveryLauncher = { intent, onComplete ->
                onDriveAuthRecoveryComplete = onComplete
                driveAuthRecoveryLauncher.launch(intent)
            }
            actions.imagePickerLauncher = { launchImagePicker() }
            actions.cameraLauncher = { launchCamera() }
            actions.notificationPermissionRequester = { onResult ->
                onNotificationPermissionResult = onResult
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onResult(true)
                }
            }
            actions.runtimePermissionRequester = { permission, onGranted ->
                val androidPermission = when (permission) {
                    Permission.CAMERA -> Manifest.permission.CAMERA
                    Permission.RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
                    else -> null
                }
                if (androidPermission != null) {
                    onRuntimePermissionGranted = onGranted
                    runtimePermissionLauncher.launch(androidPermission)
                }
            }
        }

        setContent {
            App(initialTab = launchTab)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeNavigationTab(intent)?.let { tab ->
            MainTabNavigation.request(tab)
        }
        intent.data?.toString()?.let { uriString ->
            deepLinkDispatcher.dispatch(uriString)
        }
    }

    private fun consumeNavigationTab(intent: Intent?): AppTab? {
        val raw = intent?.getStringExtra(MainTabNavigation.EXTRA_NAVIGATE_TO) ?: return null
        intent.removeExtra(MainTabNavigation.EXTRA_NAVIGATE_TO)
        val tab = AppTab.fromName(raw) ?: return null
        MainTabNavigation.request(tab)
        return tab
    }
}
