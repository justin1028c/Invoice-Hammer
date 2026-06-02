package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.util.AppLogger

internal fun logRepositoryFailure(tag: String, operation: String, throwable: Throwable) {
    AppLogger.e(tag, "$operation failed: ${throwable.message}", throwable)
}
