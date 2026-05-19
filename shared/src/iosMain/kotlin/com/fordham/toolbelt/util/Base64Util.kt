package com.fordham.toolbelt.util

import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual fun encodeBase64(bytes: ByteArray): String {
    val data = bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }
    return data.base64EncodedStringWithOptions(0UL)
}

@OptIn(ExperimentalForeignApi::class)
actual fun decodeBase64(base64: String): ByteArray {
    val data = NSData.create(base64EncodedString = base64, options = 0UL)
        ?: return ByteArray(0)
    
    return ByteArray(data.length.toInt()).apply {
        usePinned { pinned ->
            platform.Foundation.memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
    }
}
