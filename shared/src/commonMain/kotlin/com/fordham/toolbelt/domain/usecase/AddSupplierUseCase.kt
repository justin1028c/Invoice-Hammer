package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.Supplier
import com.fordham.toolbelt.domain.repository.SupplierRepository

class AddSupplierUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(supplier: Supplier) = repository.upsertSupplier(supplier)
}
