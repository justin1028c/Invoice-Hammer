package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.*
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*

class SupplierRepositoryImpl(
    private val supplierDao: SupplierDao,
    private val receiptDao: ReceiptDao
) : SupplierRepository {

    override fun getVisibleSuppliers(): Flow<SupplierListOutcome> {
        return combine(
            supplierDao.getVisibleSuppliers(),
            receiptDao.getAllItems()
        ) { entities, receipts ->
            val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
            
            val models = entities.map { entity ->
                val supplierReceipts = receipts.filter { 
                    val isSameSupplier = it.supplierId == entity.id || 
                        (it.supplierName.isNotBlank() && it.supplierName.equals(entity.name, ignoreCase = true))
                    
                    val receiptYear = Instant.fromEpochMilliseconds(it.lastUpdated)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).year
                    
                    isSameSupplier && receiptYear == currentYear
                }
                
                val analytics = SupplierAnalytics(
                    totalSpendYtd = supplierReceipts.sumOf { it.totalPrice },
                    jobsLinked = supplierReceipts.map { it.clientName }.distinct().size,
                    avgMarkup = 0.0
                )
                entity.toDomain().copy(analytics = analytics)
            }
            SupplierListOutcome.Success(models) as SupplierListOutcome
        }.catch { emit(SupplierListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to retrieve visible suppliers"))) }
    }

    override suspend fun logPurchase(supplierId: SupplierId, amount: MoneyAmount): SupplierOutcome = try {
        val receipt = ReceiptEntity(
            description = "Supplier Purchase",
            quantity = 1.0,
            unitPrice = amount.value,
            totalPrice = amount.value,
            supplierId = supplierId.value,
            lastUpdated = Clock.System.now().toEpochMilliseconds()
        )
        receiptDao.insertItems(listOf(receipt))
        SupplierOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure(TAG, "repository", e)
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to log purchase"))
    }

    override fun getHiddenSuppliers(): Flow<SupplierListOutcome> {
        return supplierDao.getHiddenSuppliers()
            .map { entities -> SupplierListOutcome.Success(entities.map { it.toDomain() }) as SupplierListOutcome }
            .catch { emit(SupplierListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to retrieve hidden suppliers"))) }
    }

    override suspend fun upsertSupplier(supplier: Supplier): SupplierOutcome = try {
        supplierDao.insertSupplier(supplier.toEntity())
        SupplierOutcome.Success
    } catch (e: Exception) {
    logRepositoryFailure("SupplierRepositoryImpl", "repository", e)
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save supplier"))
    }

    override suspend fun replaceAllSuppliers(suppliers: List<Supplier>): SupplierOutcome = try {
        supplierDao.deleteAllSuppliers()
        if (suppliers.isNotEmpty()) {
            supplierDao.insertSuppliers(suppliers.map { it.toEntity() })
        }
        SupplierOutcome.Success
    } catch (e: Exception) {

logRepositoryFailure("SupplierRepositoryImpl", "repository", e)
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to restore suppliers"))
    }

    override suspend fun hideSupplier(id: SupplierId): SupplierOutcome = try {
        supplierDao.hideSupplier(id.value)
        SupplierOutcome.Success
    } catch (e: Exception) {
    logRepositoryFailure("SupplierRepositoryImpl", "repository", e)
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to hide supplier"))
    }

    override suspend fun restoreSupplier(id: SupplierId): SupplierOutcome = try {
        supplierDao.restoreSupplier(id.value)
        SupplierOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure(TAG, "repository", e)
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to restore supplier"))
    }

    override suspend fun updateOrder(id: SupplierId, newOrder: Int): SupplierOutcome = try {
        supplierDao.updateOrder(id.value, newOrder)
        SupplierOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure(TAG, "repository", e)
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to update supplier order"))
    }

    override suspend fun togglePin(id: SupplierId, isPinned: Boolean): SupplierOutcome = try {
        val supplier = supplierDao.getVisibleSuppliersOnce().find { it.id == id.value }
        supplier?.let {
            supplierDao.insertSupplier(it.copy(isPinned = isPinned))
        }
        SupplierOutcome.Success
    } catch (e: Exception) {
            logRepositoryFailure(TAG, "repository", e)
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to pin supplier"))
    }

    override suspend fun seedDefaultSuppliers(): SupplierOutcome = try {
        val suppliers = supplierDao.getVisibleSuppliersOnce()
        if (suppliers.isEmpty()) {
            supplierDao.insertSuppliers(com.fordham.toolbelt.data.defaultSupplierEntities())
        }
        SupplierOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure(TAG, "seedDefaultSuppliers", e)
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to seed default suppliers"))
    }
}

private const val TAG = "SupplierRepository"
