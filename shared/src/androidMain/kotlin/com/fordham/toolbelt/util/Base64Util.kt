package com.fordham.toolbelt.util

import android.util.Base64

actual fun encodeBase64(bytes: ByteArray): String {
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

actual fun decodeBase64(base64: String): ByteArray {
    return Base64.decode(base64, Base64.DEFAULT)
}
