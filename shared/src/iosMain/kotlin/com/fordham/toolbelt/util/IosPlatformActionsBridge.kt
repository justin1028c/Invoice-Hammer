package com.fordham.toolbelt.util

import kotlin.native.concurrent.ThreadLocal

interface IosPlatformActionsBridge {
    fun signInWithGoogle(onSuccess: (String) -> Unit, onError: (String) -> Unit)
}

@ThreadLocal
object IosPlatformActionsServiceProvider {
    var bridge: IosPlatformActionsBridge? = null
}
