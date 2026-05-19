package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.ReceiptDao
import com.fordham.toolbelt.data.toDomain
import com.fordham.toolbelt.data.toEntity
import com.fordham.toolbelt.domain.model.ReceiptOutcome
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch

class RoomReceiptRepository(
    private val receiptDao: ReceiptDao
) : ReceiptRepository {
    override val allItems: Flow<ReceiptListOutcome> = 
        receiptDao.getAllItems()
            .map { list -> ReceiptListOutcome.Success(list.map { it.toDomain() }) as ReceiptListOutcome }
            .catch { emit(ReceiptListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to list receipts"))) }

    override suspend fun insertItem(item: ReceiptItem): ReceiptOutcome = try { 
        receiptDao.insertItems(listOf(item.toEntity()))
        ReceiptOutcome.Success
    } catch (e: Exception) {
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert receipt"))
    }

    override suspend fun insertItems(items: List<ReceiptItem>): ReceiptOutcome = try {
        receiptDao.insertItems(items.map { it.toEntity() })
        ReceiptOutcome.Success
    } catch (e: Exception) {
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert receipts"))
    }

    override suspend fun deleteItem(item: ReceiptItem): ReceiptOutcome = try { 
        receiptDao.deleteItem(item.toEntity())
        ReceiptOutcome.Success
    } catch (e: Exception) {
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to delete receipt"))
    }

    override suspend fun deleteAllItems(): ReceiptOutcome = try {
        receiptDao.deleteAllItems()
        ReceiptOutcome.Success
    } catch (e: Exception) {
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to purge receipts"))
    }

    override fun getItemsByClient(clientName: String): Flow<ReceiptListOutcome> = 
        receiptDao.getItemsByClient(clientName)
            .map { list -> ReceiptListOutcome.Success(list.map { it.toDomain() }) as ReceiptListOutcome }
            .catch { emit(ReceiptListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to retrieve receipts by client"))) }

    override fun getUnassignedReceipts(): Flow<ReceiptListOutcome> =
        receiptDao.getUnassignedReceipts()
            .map { list -> ReceiptListOutcome.Success(list.map { it.toDomain() }) as ReceiptListOutcome }
            .catch { emit(ReceiptListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to retrieve unassigned receipts"))) }

    override suspend fun updateItem(item: ReceiptItem): ReceiptOutcome = try {
        receiptDao.updateItem(item.toEntity())
        ReceiptOutcome.Success
    } catch (e: Exception) {
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to update receipt"))
    }
}