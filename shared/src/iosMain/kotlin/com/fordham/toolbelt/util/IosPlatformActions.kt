package com.fordham.toolbelt.util

import platform.Foundation.*
import platform.UIKit.*
import platform.AVFoundation.*
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.darwin.*
import platform.UserNotifications.*

class IosPlatformActions : PlatformActions {
    private fun runOnMain(block: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) {
            block()
        }
    }

    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }

    override fun shareFile(path: String, title: String) {
        val url = NSURL.fileURLWithPath(path)
        val activityViewController = UIActivityViewController(listOf(url), null)
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
    }

    override fun openPdf(path: String) {
        // QuickLook would be better, but UIDocumentInteractionController is simpler for a bridge
        val url = NSURL.fileURLWithPath(path)
        val interactionController = UIDocumentInteractionController.interactionControllerWithURL(url)
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.let {
            interactionController.delegate = null // Simple case
            interactionController.presentPreviewAnimated(true)
        }
    }

    override fun callPhone(phoneNumber: String) {
        openUrl("tel:$phoneNumber")
    }

    override fun sendEmail(email: String) {
        openUrl("mailto:$email")
    }

    override fun requestPermission(permission: String, onGranted: () -> Unit) {
        when (permission) {
            Permission.RECORD_AUDIO -> {
                AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                    if (granted) runOnMain(onGranted)
                }
            }
            Permission.CAMERA -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    if (granted) runOnMain(onGranted)
                }
            }
            else -> onGranted()
        }
    }

    override fun isPermissionGranted(permission: String): Boolean {
        return when (permission) {
            Permission.RECORD_AUDIO -> AVAudioSession.sharedInstance().recordPermission() == AVAudioSessionRecordPermissionGranted
            Permission.CAMERA -> AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
            else -> true
        }
    }

    override fun showToast(message: String) {
        // iOS doesn't have "Toasts". Usually done with a temporary alert or a custom view.
        val alert = UIAlertController.alertControllerWithTitle(null, message, UIAlertControllerStyleAlert)
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(alert, animated = true) {
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 2_000_000_000L), dispatch_get_main_queue()) {
                alert.dismissViewControllerAnimated(true, null)
            }
        }
    }

    override fun launchApp(packageName: String, fallbackUrl: String) {
        // iOS uses Custom URL Schemes, not package names.
        openUrl(fallbackUrl)
    }

    override fun signInWithGoogle(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val bridge = IosPlatformActionsServiceProvider.bridge
        if (bridge != null) {
            bridge.signInWithGoogle(
                onSuccess = { token -> runOnMain { onSuccess(token) } },
                onError = { message -> runOnMain { onError(message) } }
            )
        } else {
            onError("IosPlatformActionsBridge not initialized")
        }
    }

    override fun signOut() {
        // Sign out logic
    }

    override fun authenticateBiometric(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val context = LAContext()
        var error: NSError? = null
        if (context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)) {
            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = title
            ) { success, evalError ->
                if (success) {
                    runOnMain(onSuccess)
                } else {
                    runOnMain {
                        onError(evalError?.localizedDescription ?: "Authentication failed")
                    }
                }
            }
        } else {
            onError("Biometrics not available")
        }
    }

    override fun isBiometricAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)
    }

    private var imagePickerDelegate: ImagePickerDelegate? = null

    override fun pickImage(onResult: (String?) -> Unit) {
        if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceTypePhotoLibrary)) {
            presentImagePicker(UIImagePickerControllerSourceTypePhotoLibrary, onResult)
        } else {
            showToast("Photo library not available")
            onResult(null)
        }
    }

    override fun capturePhoto(onResult: (String?) -> Unit) {
        if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceTypeCamera)) {
            presentImagePicker(UIImagePickerControllerSourceTypeCamera, onResult)
        } else {
            showToast("Camera not available on this device")
            onResult(null)
        }
    }

    private fun presentImagePicker(sourceType: UIImagePickerControllerSourceType, onResult: (String?) -> Unit) {
        val picker = UIImagePickerController()
        picker.sourceType = sourceType
        
        imagePickerDelegate = ImagePickerDelegate { path ->
            onResult(path)
            imagePickerDelegate = null // Clear reference when done
        }
        
        picker.delegate = imagePickerDelegate

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootViewController != null) {
            rootViewController.presentViewController(picker, animated = true, completion = null)
        } else {
            onResult(null)
        }
    }

    override fun scheduleNotification(id: String, title: String, body: String, delayMillis: Long) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
            if (granted) {
                val content = UNMutableNotificationContent().apply {
                    setTitle(title)
                    setBody(body)
                    setSound(UNNotificationSound.defaultSound)
                }

                val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                    delayMillis.toDouble() / 1000.0,
                    repeats = false
                )

                val request = UNNotificationRequest.requestWithIdentifier(id, content, trigger)
                center.addNotificationRequest(request, null)
            }
        }
    }
}

class ImagePickerDelegate(
    private val onImagePicked: (String?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        if (image != null) {
            val data = UIImageJPEGRepresentation(image, 0.8)
            val tempDir = NSTemporaryDirectory()
            val fileName = NSUUID().UUIDString + ".jpg"
            val path = tempDir + fileName
            data?.writeToFile(path, atomically = true)
            onImagePicked(path)
        } else {
            onImagePicked(null)
        }
        picker.dismissViewControllerAnimated(true, null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        onImagePicked(null)
        picker.dismissViewControllerAnimated(true, null)
    }
}
