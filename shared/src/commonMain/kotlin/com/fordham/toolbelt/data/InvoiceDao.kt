package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY lastUpdated DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY lastUpdated DESC")
    suspend fun getAllInvoicesOnce(): List<InvoiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<InvoiceEntity>)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices WHERE LOWER(clientName) = LOWER(:clientName)")
    fun getInvoicesByClient(clientName: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: String): InvoiceEntity?

    @Query(
        """
        SELECT * FROM invoices
        WHERE clientName LIKE '%' || :query || '%'
           OR id LIKE '%' || :query || '%'
           OR itemsSummary LIKE '%' || :query || '%'
        ORDER BY lastUpdated DESC
        LIMIT 25
        """
    )
    suspend fun searchInvoices(query: String): List<InvoiceEntity>

    @Query("DELETE FROM invoices")
    suspend fun deleteAllInvoices()
}
