package com.fordham.toolbelt.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.kCCHmacAlgSHA256

@OptIn(ExperimentalForeignApi::class)
internal actual object HmacSha256 {
    actual fun hexDigest(secret: ByteArray, message: ByteArray): String {
        val output = ByteArray(32)
        secret.usePinned { secretPinned ->
            message.usePinned { messagePinned ->
                output.usePinned { outputPinned ->
                    CCHmac(
                        kCCHmacAlgSHA256,
                        secretPinned.addressOf(0),
                        secret.size.convert(),
                        messagePinned.addressOf(0),
                        message.size.convert(),
                        outputPinned.addressOf(0)
                    )
                }
            }
        }
        return output.joinToString(separator = "") { byte ->
            (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
    }
}
