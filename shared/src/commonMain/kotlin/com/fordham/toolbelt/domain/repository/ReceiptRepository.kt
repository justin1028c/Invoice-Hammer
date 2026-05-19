package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.ReceiptOutcome
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.ReceiptItem
import kotlinx.coroutines.flow.Flow

interface ReceiptRepository {
    val allItems: Flow<ReceiptListOutcome>
    suspend fun insertItem(item: ReceiptItem): ReceiptOutcome
    suspend fun insertItems(items: List<ReceiptItem>): ReceiptOutcome
    suspend fun deleteItem(item: ReceiptItem): ReceiptOutcome
    suspend fun deleteAllItems(): ReceiptOutcome
    fun getItemsByClient(clientName: String): Flow<ReceiptListOutcome>
    fun getUnassignedReceipts(): Flow<ReceiptListOutcome>
    suspend fun updateItem(item: ReceiptItem): ReceiptOutcome
}