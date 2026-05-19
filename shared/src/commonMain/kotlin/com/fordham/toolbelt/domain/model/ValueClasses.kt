package com.fordham.toolbelt.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class ClientId(val value: String)

@JvmInline
value class InvoiceId(val value: String)

@JvmInline
value class SupplierId(val value: String)

@JvmInline
value class ReceiptId(val value: String)

@JvmInline
value class NoteId(val value: String)

@JvmInline
value class PhotoId(val value: String)

@JvmInline
value class EmailAddress(val value: String)

@JvmInline
value class PhoneNumber(val value: String)

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
