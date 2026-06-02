package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.InvoiceOutcome
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoiceId
import kotlinx.coroutines.flow.Flow

interface InvoiceRepository {
    val allInvoices: Flow<List<Invoice>>
    suspend fun insertInvoice(invoice: Invoice): InvoiceOutcome
    suspend fun insertInvoices(invoices: List<Invoice>): InvoiceOutcome
    suspend fun updateInvoice(invoice: Invoice): InvoiceOutcome
    suspend fun deleteInvoice(invoice: Invoice): InvoiceOutcome
    suspend fun getInvoiceById(id: InvoiceId): Invoice?
    fun getInvoicesByClient(clientName: String): Flow<List<Invoice>>
    suspend fun searchInvoices(query: String): List<Invoice>
    suspend fun deleteAllInvoices(): InvoiceOutcome
}