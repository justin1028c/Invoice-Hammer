package com.fordham.toolbelt.util

expect fun encodeBase64(bytes: ByteArray): String
expect fun decodeBase64(base64: String): ByteArray
