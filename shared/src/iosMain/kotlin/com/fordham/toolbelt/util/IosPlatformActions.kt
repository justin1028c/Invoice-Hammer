package com.fordham.toolbelt.util

import platform.Foundation.*
import platform.UIKit.*
import platform.AVFoundation.*
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.darwin.*
import com.fordham.toolbelt.navigation.MainTabNavigation
import platform.UserNotifications.*

class IosPlatformActions : PlatformActions {
    private var notificationsGranted: Boolean = false

    init {
        refreshNotificationPermissionStatus()
    }

    private fun runOnMain(block: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) {
            block()
        }
    }

    private fun refreshNotificationPermissionStatus() {
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                notificationsGranted = when (settings.authorizationStatus) {
                    UNAuthorizationStatusAuthorized,
                    UNAuthorizationStatusProvisional,
                    UNAuthorizationStatusEphemeral -> true
                    else -> false
                }
            }
    }

    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
        }
    }

    private fun getRootViewController(): UIViewController? {
        val scenes = UIApplication.sharedApplication.connectedScenes
        for (scene in scenes.allObjects) {
            val windowScene = scene as? UIWindowScene
            if (windowScene != null) {
                for (window in windowScene.windows) {
                    val uiWindow = window as? UIWindow
                    if (uiWindow != null && uiWindow.isKeyWindow()) {
                        return uiWindow.rootViewController
                    }
                }
            }
        }
        @Suppress("DEPRECATION")
        return UIApplication.sharedApplication.keyWindow?.rootViewController
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
        val url = NSURL.fileURLWithPath(path)
        val items = mutableListOf<Any>(url)
        if (body.isNotBlank()) items.add(body)
        val activityViewController = UIActivityViewController(items, null)
        val rootViewController = getRootViewController()
        rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
    }

    override fun openPdf(path: String) {
        val url = NSURL.fileURLWithPath(path)
        val interactionController = UIDocumentInteractionController.interactionControllerWithURL(url)
        val rootViewController = getRootViewController()
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
            Permission.POST_NOTIFICATIONS -> {
                UNUserNotificationCenter.currentNotificationCenter()
                    .requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
                        notificationsGranted = granted
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
            Permission.POST_NOTIFICATIONS -> {
                refreshNotificationPermissionStatus()
                notificationsGranted
            }
            else -> true
        }
    }

    override fun showToast(message: String) {
        // iOS doesn't have "Toasts". Usually done with a temporary alert or a custom view.
        val alert = UIAlertController.alertControllerWithTitle(null, message, UIAlertControllerStyleAlert)
        val rootViewController = getRootViewController()
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
        val policy = when {
            context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null) ->
                LAPolicyDeviceOwnerAuthenticationWithBiometrics
            context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = null) ->
                LAPolicyDeviceOwnerAuthentication
            else -> {
                onError("Device authentication not available")
                return
            }
        }

        context.evaluatePolicy(
            policy,
            localizedReason = subtitle.ifBlank { title }
        ) { success, evalError ->
            if (success) {
                runOnMain(onSuccess)
            } else {
                runOnMain {
                    onError(evalError?.localizedDescription ?: "Authentication failed")
                }
            }
        }
    }

    override fun isBiometricAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null) ||
            context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)
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

        val rootViewController = getRootViewController()
        if (rootViewController != null) {
            rootViewController.presentViewController(picker, animated = true, completion = null)
        } else {
            onResult(null)
        }
    }

    override fun scheduleNotification(id: String, title: String, body: String, delayMillis: Long) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
            notificationsGranted = granted
            if (!granted) return@requestAuthorizationWithOptions

                val content = UNMutableNotificationContent().apply {
                    setTitle(title)
                    setBody(body)
                    setSound(UNNotificationSound.defaultSound)
                    if (id == UnpaidInvoiceReminders.WORK_ID) {
                        setUserInfo(mapOf(MainTabNavigation.EXTRA_NAVIGATE_TO to MainTabNavigation.TARGET_HISTORY))
                    }
                }

            if (id == UnpaidInvoiceReminders.WORK_ID) {
                val firstDelay = maxOf(delayMillis.toDouble() / 1000.0, 60.0)
                val firstTrigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                    firstDelay,
                    repeats = false
                )
                val firstRequest = UNNotificationRequest.requestWithIdentifier(
                    "$id:first",
                    content,
                    firstTrigger
                )
                center.addNotificationRequest(firstRequest, null)

                val dailyInterval = UnpaidInvoiceReminders.REPEAT_INTERVAL_MS.toDouble() / 1000.0
                val dailyTrigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                    dailyInterval,
                    repeats = true
                )
                val dailyRequest = UNNotificationRequest.requestWithIdentifier(id, content, dailyTrigger)
                center.addNotificationRequest(dailyRequest, null)
            } else {
                val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                    maxOf(delayMillis.toDouble() / 1000.0, 1.0),
                    repeats = false
                )
                val request = UNNotificationRequest.requestWithIdentifier(id, content, trigger)
                center.addNotificationRequest(request, null)
            }
        }
    }

    override fun cancelScheduledNotification(id: String) {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(id, "$id:first"))
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
            val data = image.jpegData(compressionQuality = 0.8)
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
