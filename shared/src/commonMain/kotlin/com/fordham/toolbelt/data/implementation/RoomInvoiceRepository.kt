package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.DatabaseProvider
import com.fordham.toolbelt.data.toDomain
import com.fordham.toolbelt.data.toEntity
import com.fordham.toolbelt.domain.model.InvoiceOutcome
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.data.SyncQueueEntity
import com.fordham.toolbelt.util.PlatformActions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

public class RoomInvoiceRepository(
    private val databaseProvider: DatabaseProvider,
    private val platformActions: PlatformActions
) : InvoiceRepository {

    private suspend fun invoiceDao() = databaseProvider.getDatabase().invoiceDao()
    private suspend fun syncQueueDao() = databaseProvider.getDatabase().syncQueueDao()

    override val allInvoices: Flow<List<Invoice>> = flow {
        val dao = invoiceDao()
        emitAll(
            dao.getAllInvoices().map { list -> list.map { it.toDomain() } }
        )
    }

    override suspend fun insertInvoice(invoice: Invoice): InvoiceOutcome = try {
        invoiceDao().insertInvoice(invoice.toEntity())
        syncQueueDao().enqueue(
            SyncQueueEntity(
                operationType = "BACKUP",
                createdAtMillis = Clock.System.now().toEpochMilliseconds()
            )
        )
        platformActions.triggerBackgroundSync()
        InvoiceOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomInvoiceRepository", "repository", e)
        InvoiceOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert invoice"))
    }
    
    override suspend fun insertInvoices(invoices: List<Invoice>): InvoiceOutcome = try {
        invoiceDao().insertInvoices(invoices.map { it.toEntity() })
        InvoiceOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomInvoiceRepository", "repository", e)
        InvoiceOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert invoices"))
    }

    override suspend fun updateInvoice(invoice: Invoice): InvoiceOutcome = try {
        invoiceDao().updateInvoice(invoice.toEntity())
        syncQueueDao().enqueue(
            SyncQueueEntity(
                operationType = "BACKUP",
                createdAtMillis = Clock.System.now().toEpochMilliseconds()
            )
        )
        platformActions.triggerBackgroundSync()
        InvoiceOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomInvoiceRepository", "repository", e)
        InvoiceOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to update invoice"))
    }

    override suspend fun deleteInvoice(invoice: Invoice): InvoiceOutcome = try {
        invoiceDao().deleteInvoice(invoice.toEntity())
        syncQueueDao().enqueue(
            SyncQueueEntity(
                operationType = "BACKUP",
                createdAtMillis = Clock.System.now().toEpochMilliseconds()
            )
        )
        platformActions.triggerBackgroundSync()
        InvoiceOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomInvoiceRepository", "repository", e)
        InvoiceOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to delete invoice"))
    }

    override suspend fun deleteAllInvoices(): InvoiceOutcome = try {
        invoiceDao().deleteAllInvoices()
        InvoiceOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomInvoiceRepository", "repository", e)
        InvoiceOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed bulk database purge transaction"))
    }

    override suspend fun getInvoiceById(id: InvoiceId): Invoice? =
        invoiceDao().getInvoiceById(id.value)?.toDomain()

    override fun getInvoicesByClient(clientName: String): Flow<List<Invoice>> = flow {
        val dao = invoiceDao()
        emitAll(
            dao.getInvoicesByClient(clientName).map { list -> list.map { it.toDomain() } }
        )
    }

    override suspend fun searchInvoices(query: String): List<Invoice> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return invoiceDao().getAllInvoicesOnce().take(25).map { it.toDomain() }
        }
        return invoiceDao().searchInvoices(trimmed).map { it.toDomain() }
    }
}

private const val TAG = "RoomInvoiceRepository"
