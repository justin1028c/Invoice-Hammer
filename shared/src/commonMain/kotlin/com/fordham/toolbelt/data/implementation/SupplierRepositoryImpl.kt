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
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save supplier"))
    }

    override suspend fun hideSupplier(id: SupplierId): SupplierOutcome = try {
        supplierDao.hideSupplier(id.value)
        SupplierOutcome.Success
    } catch (e: Exception) {
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to hide supplier"))
    }

    override suspend fun restoreSupplier(id: SupplierId): SupplierOutcome = try {
        supplierDao.restoreSupplier(id.value)
        SupplierOutcome.Success
    } catch (e: Exception) {
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to restore supplier"))
    }

    override suspend fun updateOrder(id: SupplierId, newOrder: Int): SupplierOutcome = try {
        supplierDao.updateOrder(id.value, newOrder)
        SupplierOutcome.Success
    } catch (e: Exception) {
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to update supplier order"))
    }

    override suspend fun togglePin(id: SupplierId, isPinned: Boolean): SupplierOutcome = try {
        val supplier = supplierDao.getVisibleSuppliers().first().find { it.id == id.value }
        supplier?.let {
            supplierDao.insertSupplier(it.copy(isPinned = isPinned))
        }
        SupplierOutcome.Success
    } catch (e: Exception) {
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to pin supplier"))
    }

    override suspend fun seedDefaultSuppliers(): SupplierOutcome = try {
        val suppliers = supplierDao.getVisibleSuppliers().first()
        if (suppliers.isEmpty()) {
            val defaults = listOf(
                SupplierEntity(
                    id = "home_depot",
                    name = "Home Depot",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.thehomedepot",
                    webUrl = "https://www.homedepot.com",
                    displayOrder = 0,
                    logoResName = "logo_home_depot",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "lowes",
                    name = "Lowe's",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.lowes.android",
                    webUrl = "https://www.lowes.com",
                    displayOrder = 1,
                    logoResName = "logo_lowes",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "ace",
                    name = "Ace Hardware",
                    category = SupplierCategory.HARDWARE.name,
                    packageName = "com.acehardware.rewards",
                    webUrl = "https://www.acehardware.com",
                    displayOrder = 2,
                    logoResName = "logo_ace",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "menards",
                    name = "Menards",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.menards.mobile",
                    webUrl = "https://www.menards.com",
                    displayOrder = 3,
                    logoResName = "logo_menards",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "ferguson",
                    name = "Ferguson",
                    category = SupplierCategory.PLUMBING.name,
                    packageName = "com.ferguson.mobile",
                    webUrl = "https://www.ferguson.com",
                    displayOrder = 4,
                    logoResName = "logo_ferguson",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "sherwin",
                    name = "Sherwin-Williams",
                    category = SupplierCategory.PAINT.name,
                    packageName = "com.sherwin.probuyplus",
                    webUrl = "https://www.sherwin-williams.com",
                    displayOrder = 5,
                    logoResName = "logo_sherwin",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "grainger",
                    name = "Grainger",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.grainger.graingerapp",
                    webUrl = "https://www.grainger.com",
                    displayOrder = 6,
                    logoResName = "logo_grainger",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "abc_supply",
                    name = "ABC Supply",
                    category = SupplierCategory.ROOFING.name,
                    packageName = "com.abcsupply.myabcsupply",
                    webUrl = "https://www.abcsupply.com",
                    displayOrder = 7,
                    logoResName = "logo_abc",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "graybar",
                    name = "Graybar",
                    category = SupplierCategory.ELECTRICAL.name,
                    packageName = "com.graybar.mobile",
                    webUrl = "https://www.graybar.com",
                    displayOrder = 8,
                    logoResName = "logo_graybar",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "siteone",
                    name = "SiteOne",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.siteone.mobile",
                    webUrl = "https://www.siteone.com",
                    displayOrder = 9,
                    logoResName = "logo_siteone",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "amazon_biz",
                    name = "Amazon Business",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.amazon.mShop.android.business.shopping",
                    webUrl = "https://www.amazon.com/business",
                    displayOrder = 10,
                    logoResName = "logo_amazon",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "northern_tool",
                    name = "Northern Tool",
                    category = SupplierCategory.HARDWARE.name,
                    packageName = "com.multiservice.ntca",
                    webUrl = "https://www.northerntool.com",
                    displayOrder = 11,
                    logoResName = "logo_northern",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "sunbelt",
                    name = "Sunbelt Rentals",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.sunbeltrentals.app",
                    webUrl = "https://www.sunbeltrentals.com",
                    displayOrder = 12,
                    logoResName = "logo_sunbelt",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "hilti",
                    name = "Hilti",
                    category = SupplierCategory.FASTENERS.name,
                    packageName = "com.hilti.mobile.hiltionline",
                    webUrl = "https://www.hilti.com",
                    displayOrder = 13,
                    logoResName = "logo_hilti",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "mcmaster",
                    name = "McMaster-Carr",
                    category = SupplierCategory.FASTENERS.name,
                    packageName = "com.mcmaster.android",
                    webUrl = "https://www.mcmaster.com",
                    displayOrder = 14,
                    logoResName = "logo_mcmaster",
                    isDefault = true
                )
            )
            supplierDao.insertSuppliers(defaults)
        }
        SupplierOutcome.Success
    } catch (e: Exception) {
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to seed default suppliers"))
    }
}
