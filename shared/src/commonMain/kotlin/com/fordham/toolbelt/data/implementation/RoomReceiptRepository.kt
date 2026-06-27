package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.DatabaseProvider
import com.fordham.toolbelt.data.toDomain
import com.fordham.toolbelt.data.toEntity
import com.fordham.toolbelt.domain.model.ReceiptOutcome
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.data.SyncQueueEntity
import com.fordham.toolbelt.util.PlatformActions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.datetime.Clock

public class RoomReceiptRepository(
    private val databaseProvider: DatabaseProvider,
    private val platformActions: PlatformActions
) : ReceiptRepository {

    private suspend fun receiptDao() = databaseProvider.getDatabase().receiptDao()
    private suspend fun syncQueueDao() = databaseProvider.getDatabase().syncQueueDao()

    override val allItems: Flow<ReceiptListOutcome> = flow {
        val dao = receiptDao()
        emitAll(
            dao.getAllItems()
                .map { list -> ReceiptListOutcome.Success(list.map { it.toDomain() }) as ReceiptListOutcome }
        )
    }.catch { emit(ReceiptListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to list receipts"))) }

    override suspend fun insertItem(item: ReceiptItem): ReceiptOutcome = try { 
        receiptDao().insertItems(listOf(item.toEntity()))
        syncQueueDao().enqueue(
            SyncQueueEntity(
                operationType = "BACKUP",
                createdAtMillis = Clock.System.now().toEpochMilliseconds()
            )
        )
        platformActions.triggerBackgroundSync()
        ReceiptOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomReceiptRepository", "repository", e)
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert receipt"))
    }

    override suspend fun insertItems(items: List<ReceiptItem>): ReceiptOutcome = try {
        receiptDao().insertItems(items.map { it.toEntity() })
        syncQueueDao().enqueue(
            SyncQueueEntity(
                operationType = "BACKUP",
                createdAtMillis = Clock.System.now().toEpochMilliseconds()
            )
        )
        platformActions.triggerBackgroundSync()
        ReceiptOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomReceiptRepository", "repository", e)
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert receipts"))
    }

    override suspend fun deleteItem(item: ReceiptItem): ReceiptOutcome = try { 
        receiptDao().deleteItem(item.toEntity())
        syncQueueDao().enqueue(
            SyncQueueEntity(
                operationType = "BACKUP",
                createdAtMillis = Clock.System.now().toEpochMilliseconds()
            )
        )
        platformActions.triggerBackgroundSync()
        ReceiptOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomReceiptRepository", "repository", e)
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to delete receipt"))
    }

    override suspend fun deleteAllItems(): ReceiptOutcome = try {
        receiptDao().deleteAllItems()
        ReceiptOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomReceiptRepository", "repository", e)
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to purge receipts"))
    }

    override fun getItemsByClient(clientName: String): Flow<ReceiptListOutcome> = flow {
        val dao = receiptDao()
        emitAll(
            dao.getItemsByClient(clientName)
                .map { list -> ReceiptListOutcome.Success(list.map { it.toDomain() }) as ReceiptListOutcome }
        )
    }.catch { emit(ReceiptListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to retrieve receipts by client"))) }

    override fun getUnassignedReceipts(): Flow<ReceiptListOutcome> = flow {
        val dao = receiptDao()
        emitAll(
            dao.getUnassignedReceipts()
                .map { list -> ReceiptListOutcome.Success(list.map { it.toDomain() }) as ReceiptListOutcome }
        )
    }.catch { emit(ReceiptListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to retrieve unassigned receipts"))) }

    override suspend fun updateItem(item: ReceiptItem): ReceiptOutcome = try {
        receiptDao().updateItem(item.toEntity())
        syncQueueDao().enqueue(
            SyncQueueEntity(
                operationType = "BACKUP",
                createdAtMillis = Clock.System.now().toEpochMilliseconds()
            )
        )
        platformActions.triggerBackgroundSync()
        ReceiptOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomReceiptRepository", "repository", e)
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to update receipt"))
    }
}

private const val TAG = "RoomReceiptRepository"
