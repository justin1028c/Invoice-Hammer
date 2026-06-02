package com.fordham.toolbelt.util

internal expect object HmacSha256 {
    fun hexDigest(secret: ByteArray, message: ByteArray): String
}
