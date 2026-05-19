package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipt_items")
    fun getAllItems(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipt_items WHERE clientName = :clientName OR clientName = 'General'")
    fun getItemsByClient(clientName: String): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipt_items WHERE (clientName = '' OR clientName = 'General') AND linkedInvoiceId IS NULL")
    fun getUnassignedReceipts(): Flow<List<ReceiptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ReceiptEntity>)

    @Update
    suspend fun updateItem(item: ReceiptEntity)

    @Delete
    suspend fun deleteItem(item: ReceiptEntity)

    @Query("DELETE FROM receipt_items")
    suspend fun deleteAllItems()
}
