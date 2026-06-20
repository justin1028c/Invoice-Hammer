package com.fordham.toolbelt.util

import kotlin.native.concurrent.ThreadLocal

import platform.UIKit.UIImage
import platform.Foundation.NSData

interface IosPlatformActionsBridge {
    fun signInWithGoogle(onSuccess: (String) -> Unit, onError: (String) -> Unit)
    fun getJpegData(image: UIImage, compressionQuality: Double): NSData?
    fun submitBackgroundSyncTask(): Boolean
}

@ThreadLocal
object IosPlatformActionsServiceProvider {
    var bridge: IosPlatformActionsBridge? = null
}
