package com.fordham.toolbelt.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal actual object HmacSha256 {
    actual fun hexDigest(secret: ByteArray, message: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        return mac.doFinal(message).joinToString(separator = "") { byte ->
            (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
    }
}
