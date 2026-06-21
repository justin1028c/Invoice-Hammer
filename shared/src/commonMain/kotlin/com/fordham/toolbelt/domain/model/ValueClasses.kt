package com.fordham.toolbelt.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class ClientId(val value: String) {
    init {
        require(value.isNotBlank()) { "Client ID cannot be blank." }
    }
}

@JvmInline
value class InvoiceId(val value: String) {
    init {
        require(value.isNotBlank()) { "Invoice ID cannot be blank." }
    }
}

@JvmInline
value class SupplierId(val value: String) {
    init {
        require(value.isNotBlank()) { "Supplier ID cannot be blank." }
    }
}

@JvmInline
value class ReceiptId(val value: String) {
    init {
        require(value.isNotBlank()) { "Receipt ID cannot be blank." }
    }
}

@JvmInline
value class NoteId(val value: String) {
    init {
        require(value.isNotBlank()) { "Note ID cannot be blank." }
    }
}

@JvmInline
value class PhotoId(val value: String) {
    init {
        require(value.isNotBlank()) { "Photo ID cannot be blank." }
    }
}

@JvmInline
value class EmailAddress(val value: String) {
    init {
        if (value.isNotEmpty()) {
            require(value.contains("@") && value.contains(".")) { "Invalid email address format." }
        }
    }
}

@JvmInline
value class PhoneNumber(val value: String) {
    init {
        if (value.isNotEmpty()) {
            require(value.length >= 7) { "Phone number must be at least 7 characters." }
        }
    }
}

@JvmInline
value class MoneyAmount(val value: Double) {
    init {
        require(value >= 0.0) { "Money amount cannot be negative." }
    }
}

@JvmInline
value class BackupFileName(val value: String) {
    init {
        require(value.isNotBlank()) { "Backup file name cannot be blank." }
    }
}

@JvmInline
value class BackupPayload(val bytes: ByteArray) {
    init {
        require(bytes.isNotEmpty()) { "Backup payload cannot be empty." }
    }
}

@JvmInline
value class ClientName(val value: String) {
    init {
        require(value.isNotBlank()) { "Client name cannot be blank." }
    }
}

@JvmInline
value class ClientAddress(val value: String) {
    init {
        if (value.isNotEmpty()) {
            require(value.isNotBlank()) { "Client address cannot be blank." }
        }
    }
}

@JvmInline
value class TaxRatePercent(val value: Double) {
    init {
        require(value in 0.0..100.0) { "Tax rate must be between 0 and 100 percent." }
    }
}

@JvmInline
value class ItemsSummary(val value: String) {
    init {
        require(value.isNotBlank()) { "Items summary cannot be blank." }
    }
}

@JvmInline
value class PdfFilePath(val value: String) {
    init {
        if (value.isNotEmpty()) {
            require(value.isNotBlank()) { "PDF file path cannot be blank." }
        }
    }
}

@JvmInline
value class MediaUri(val value: String) {
    init {
        require(value.isNotBlank()) { "Media URI cannot be blank." }
    }
}

@JvmInline
value class DurationSeconds(val value: Long) {
    init {
        require(value >= 0L) { "Duration cannot be negative." }
    }
}

@JvmInline
value class ReceiptImagePayload(val bytes: ByteArray) {
    init {
        require(bytes.isNotEmpty()) { "Receipt image payload cannot be empty." }
    }
}

@JvmInline
value class LlmPrompt(val value: String) {
    init {
        require(value.isNotBlank()) { "LlmPrompt cannot be blank." }
    }
}

@JvmInline
value class LlmResponseText(val value: String) {
    init {
        require(value.isNotBlank()) { "LLM response text cannot be blank." }
    }
}

// Operators for MoneyAmount
operator fun MoneyAmount.plus(other: MoneyAmount): MoneyAmount = MoneyAmount(this.value + other.value)
operator fun MoneyAmount.minus(other: MoneyAmount): MoneyAmount = MoneyAmount(this.value - other.value)
operator fun MoneyAmount.times(other: Double): MoneyAmount = MoneyAmount(this.value * other)
operator fun MoneyAmount.times(other: Int): MoneyAmount = MoneyAmount(this.value * other)
operator fun MoneyAmount.div(other: Double): MoneyAmount = MoneyAmount(this.value / other)
operator fun MoneyAmount.div(other: Int): MoneyAmount = MoneyAmount(this.value / other)
operator fun MoneyAmount.compareTo(other: MoneyAmount): Int = this.value.compareTo(other.value)
operator fun MoneyAmount.compareTo(other: Double): Int = this.value.compareTo(other)

// Custom sumOf for MoneyAmount
inline fun <T> Iterable<T>.sumOf(selector: (T) -> MoneyAmount): MoneyAmount {
    var sum = 0.0
    for (element in this) {
        sum += selector(element).value
    }
    return MoneyAmount(sum)
}

// String compatibility extensions
val ClientName.length: Int get() = this.value.length
fun ClientName.isBlank(): Boolean = this.value.isBlank()
fun ClientName.isNotBlank(): Boolean = this.value.isNotBlank()
fun ClientName.lowercase(): String = this.value.lowercase()
fun ClientName.take(n: Int): String = this.value.take(n)
fun ClientName.contains(other: CharSequence, ignoreCase: Boolean = false): Boolean = this.value.contains(other, ignoreCase)
fun ClientName.replace(oldValue: String, newValue: String, ignoreCase: Boolean = false): String = this.value.replace(oldValue, newValue, ignoreCase)
fun ClientName?.orEmpty(): String = this?.value.orEmpty()

val ItemsSummary.length: Int get() = this.value.length
fun ItemsSummary.isBlank(): Boolean = this.value.isBlank()
fun ItemsSummary.isNotBlank(): Boolean = this.value.isNotBlank()
fun ItemsSummary.lowercase(): String = this.value.lowercase()
fun ItemsSummary.take(n: Int): String = this.value.take(n)
fun ItemsSummary.contains(other: CharSequence, ignoreCase: Boolean = false): Boolean = this.value.contains(other, ignoreCase)
fun ItemsSummary.replace(oldValue: String, newValue: String, ignoreCase: Boolean = false): String = this.value.replace(oldValue, newValue, ignoreCase)
fun ItemsSummary?.orEmpty(): String = this?.value.orEmpty()

val ClientAddress.length: Int get() = this.value.length
fun ClientAddress.isBlank(): Boolean = this.value.isBlank()
fun ClientAddress.isNotBlank(): Boolean = this.value.isNotBlank()
fun ClientAddress.lowercase(): String = this.value.lowercase()
fun ClientAddress.take(n: Int): String = this.value.take(n)
fun ClientAddress.contains(other: CharSequence, ignoreCase: Boolean = false): Boolean = this.value.contains(other, ignoreCase)
fun ClientAddress?.orEmpty(): String = this?.value.orEmpty()

val EmailAddress.length: Int get() = this.value.length
fun EmailAddress.isBlank(): Boolean = this.value.isBlank()
fun EmailAddress.isNotBlank(): Boolean = this.value.isNotBlank()

val PhoneNumber.length: Int get() = this.value.length
fun PhoneNumber.isBlank(): Boolean = this.value.isBlank()
fun PhoneNumber.isNotBlank(): Boolean = this.value.isNotBlank()

val PdfFilePath.length: Int get() = this.value.length
fun PdfFilePath.isBlank(): Boolean = this.value.isBlank()
fun PdfFilePath.isNotBlank(): Boolean = this.value.isNotBlank()
fun PdfFilePath.lowercase(): String = this.value.lowercase()
fun PdfFilePath.take(n: Int): String = this.value.take(n)

val DurationSeconds.length: Int get() = this.value.toString().length
fun DurationSeconds.isBlank(): Boolean = this.value.toString().isBlank()
fun DurationSeconds.isNotBlank(): Boolean = this.value.toString().isNotBlank()

