package com.fordham.toolbelt

import android.os.Bundle
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.fordham.toolbelt.util.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import org.koin.android.ext.android.inject
import java.io.File

class MainActivity : FragmentActivity() {
    
    private val platformActions: PlatformActions by inject()

    private var onGoogleSignInSuccess: ((String) -> Unit)? = null
    private var onGoogleSignInError: ((String) -> Unit)? = null

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    onGoogleSignInSuccess?.invoke(idToken)
                } else {
                    onGoogleSignInError?.invoke("ID Token is null")
                }
            } catch (e: ApiException) {
                onGoogleSignInError?.invoke(e.message ?: "Sign-in failed")
            }
        } else {
            onGoogleSignInError?.invoke("Sign-in cancelled")
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        (platformActions as? AndroidPlatformActions)?.handleImageResult(uri?.toString())
    }

    private var photoUri: Uri? = null
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
        println("MAIN_ACTIVITY: launchCamera called")
        // IMPORTANT: Must use cacheDir (not filesDir). The camera app runs in a separate
        // process and only has write permission through a FileProvider cache-path entry.
        // filesDir is private to this app and will cause the camera to silently fail.
        val photoFile = File(cacheDir, "captured_photo_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)
        println("MAIN_ACTIVITY: photoUri: $photoUri")
        cameraLauncher.launch(photoUri!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register platform hooks
        (platformActions as? AndroidPlatformActions)?.let { actions ->
            actions.activity = this
            actions.googleSignInLauncher = { success, error ->
                onGoogleSignInSuccess = success
                onGoogleSignInError = error
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(this, gso)
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
            actions.imagePickerLauncher = { launchImagePicker() }
            actions.cameraLauncher = { launchCamera() }
        }

        setContent {
            App()
        }
    }
}
