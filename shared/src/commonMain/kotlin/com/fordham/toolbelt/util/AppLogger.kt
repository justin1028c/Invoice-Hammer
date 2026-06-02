package com.fordham.toolbelt.util

/**
 * KMP-safe logging abstraction.
 * Android actual → android.util.Log
 * iOS actual     → NSLog (visible in Console.app / Xcode debugger)
 *
 * Use this everywhere in commonMain / shared code instead of println().
 * println() is JVM-friendly but produces no structured output on iOS.
 */
expect object AppLogger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
