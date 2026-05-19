package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.domain.repository.SupplierRepository

class HideSupplierUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(id: SupplierId) = repository.hideSupplier(id)
}
