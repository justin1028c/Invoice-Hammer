package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.randomUUID

data class JobPhoto(
    val id: PhotoId = PhotoId(randomUUID()),
    val invoiceId: InvoiceId,
    val localUri: String,
    val timestamp: Long = 0L
)