package com.fordham.toolbelt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentRequestDao {
    @Query("SELECT * FROM payment_requests ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<PaymentRequestEntity>>

    @Query("SELECT * FROM payment_requests ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<PaymentRequestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(request: PaymentRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(requests: List<PaymentRequestEntity>)

    @Query(
        """
        UPDATE payment_requests
        SET status = :status,
            paidAtMillis = :paidAtMillis,
            stellarTransactionHash = COALESCE(:transactionHash, stellarTransactionHash),
            stellarExplorerUrl = COALESCE(:explorerUrl, stellarExplorerUrl)
        WHERE invoiceId = :invoiceId
        """
    )
    suspend fun markInvoicePaid(
        invoiceId: String,
        status: String,
        paidAtMillis: Long,
        transactionHash: String?,
        explorerUrl: String?
    )
}
