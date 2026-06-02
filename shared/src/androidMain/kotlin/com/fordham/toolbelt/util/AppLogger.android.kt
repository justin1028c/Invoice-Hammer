package com.fordham.toolbelt.util

import android.util.Log

actual object AppLogger {
    actual fun d(tag: String, message: String) {
        try {
            Log.d(tag, message)
        } catch (_: Throwable) {
            println("[$tag] D: $message")
        }
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        try {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        } catch (_: Throwable) {
            if (throwable != null) {
                System.err.println("[$tag] E: $message - ${throwable.message}")
                throwable.printStackTrace()
            } else {
                System.err.println("[$tag] E: $message")
            }
        }
    }
}
