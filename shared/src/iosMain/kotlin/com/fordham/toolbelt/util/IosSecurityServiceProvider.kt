package com.fordham.toolbelt.util

import kotlin.native.concurrent.ThreadLocal

interface IosSecurityBridge {
    fun getDatabasePassphrase(): String
    fun saveSecret(key: String, value: String)
    fun getSecret(key: String): String?
}

@ThreadLocal
object IosSecurityServiceProvider {
    var bridge: IosSecurityBridge? = null
}
