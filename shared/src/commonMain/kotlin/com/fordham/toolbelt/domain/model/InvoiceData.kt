package com.fordham.toolbelt.domain.model

data class InvoiceData(
    val invoiceId: String,
    val clientName: String,
    val clientAddress: String,
    val items: List<LineItem>,
    val taxRate: Double,
    val date: String,
    val logoUriString: String?,
    val settings: BusinessSettings,
    val isEstimate: Boolean = false,
    val deposit: Double = 0.0,
    val jobSitePhotos: List<CapturedJobPhoto> = emptyList()
)
