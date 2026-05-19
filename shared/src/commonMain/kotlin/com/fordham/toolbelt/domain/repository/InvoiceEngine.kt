package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.InvoiceData

interface InvoiceEngine {
    fun generatePdf(data: InvoiceData): String?
}
