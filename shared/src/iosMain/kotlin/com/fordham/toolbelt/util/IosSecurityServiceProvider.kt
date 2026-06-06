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

class IosSecurityGateway : SecurityGateway {
    override fun getDatabasePassphrase(): String {
        val passphrase = IosSecurityServiceProvider.bridge?.getDatabasePassphrase()
        check(passphrase != null) { "iOS Security Bridge/Keychain is NOT initialized" }
        return passphrase
    }
}

