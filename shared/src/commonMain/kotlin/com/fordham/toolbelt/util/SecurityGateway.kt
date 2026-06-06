package com.fordham.toolbelt.util

interface SecurityGateway {
    /**
     * Retrieves or generates a 64-character passphrase for database encryption.
     */
    fun getDatabasePassphrase(): String
}
