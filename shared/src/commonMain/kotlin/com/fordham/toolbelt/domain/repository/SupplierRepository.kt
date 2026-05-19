package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.Supplier
import com.fordham.toolbelt.domain.model.SupplierOutcome
import com.fordham.toolbelt.domain.model.SupplierListOutcome
import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.domain.model.MoneyAmount
import kotlinx.coroutines.flow.Flow

interface SupplierRepository {
    fun getVisibleSuppliers(): Flow<SupplierListOutcome>
    fun getHiddenSuppliers(): Flow<SupplierListOutcome>
    suspend fun upsertSupplier(supplier: Supplier): SupplierOutcome
    suspend fun hideSupplier(id: SupplierId): SupplierOutcome
    suspend fun restoreSupplier(id: SupplierId): SupplierOutcome
    suspend fun updateOrder(id: SupplierId, newOrder: Int): SupplierOutcome
    suspend fun seedDefaultSuppliers(): SupplierOutcome
    suspend fun logPurchase(supplierId: SupplierId, amount: MoneyAmount): SupplierOutcome
    suspend fun togglePin(id: SupplierId, isPinned: Boolean): SupplierOutcome
}
