package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE isHidden = 0 ORDER BY isPinned DESC, displayOrder ASC")
    fun getVisibleSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE isHidden = 1")
    fun getHiddenSuppliers(): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<SupplierEntity>)

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("UPDATE suppliers SET isHidden = 1 WHERE id = :id")
    suspend fun hideSupplier(id: String)

    @Query("UPDATE suppliers SET isHidden = 0 WHERE id = :id")
    suspend fun restoreSupplier(id: String)

    @Query("UPDATE suppliers SET displayOrder = :newOrder WHERE id = :id")
    suspend fun updateOrder(id: String, newOrder: Int)
}
