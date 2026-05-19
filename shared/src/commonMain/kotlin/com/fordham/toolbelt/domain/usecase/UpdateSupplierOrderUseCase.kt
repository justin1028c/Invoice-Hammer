package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.domain.repository.SupplierRepository

class UpdateSupplierOrderUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(id: SupplierId, newOrder: Int) = repository.updateOrder(id, newOrder)
}
