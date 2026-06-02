package com.fordham.toolbelt.domain.model

data class DraftInvoice(
    val clientName: String = "",
    val clientAddress: String = "",
    val taxRate: Double = 7.0,
    val deposit: Double = 0.0,
    val hourlyRate: Double = 50.0,
    val logoUri: String? = null,
    val selectedCategory: String = "Drywall",
    val itemDesc: String = "",
    val itemAmt: String = "",
    val elapsedSeconds: Long = 0L,
    val startTime: Long = 0L,
    val timerRunning: Boolean = false,
    val saveToClientDirectory: Boolean = false,
    val lineItems: List<LineItem> = emptyList(),
    val capturedPhotos: List<CapturedJobPhoto> = emptyList(),
    val linkedReceiptIds: List<String> = emptyList()
)
