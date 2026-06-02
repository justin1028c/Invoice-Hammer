package com.fordham.toolbelt.util

import java.io.File

internal actual fun readAllBytes(path: String): ByteArray? {
    val file = File(path)
    if (!file.isFile || file.length() <= 0L) return null
    return runCatching { file.readBytes() }.getOrNull()
}

internal actual fun writeAllBytes(path: String, bytes: ByteArray) {
    val file = File(path)
    file.parentFile?.mkdirs()
    file.writeBytes(bytes)
}
