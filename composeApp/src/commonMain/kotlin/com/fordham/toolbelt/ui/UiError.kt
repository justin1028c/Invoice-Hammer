package com.fordham.toolbelt.ui



// =========================
// UI ERROR MODEL
// =========================

data class UiError(
    val message: String
)

fun Throwable.toUiError(): UiError {
    return UiError(
        message = when {
            this.message?.contains("Network", ignoreCase = true) == true -> "Network error. Check your connection."
            this.message?.contains("Timeout", ignoreCase = true) == true -> "Connection timed out. Please try again."
            else -> "Something went wrong. Please try again."
        }
    )
}


