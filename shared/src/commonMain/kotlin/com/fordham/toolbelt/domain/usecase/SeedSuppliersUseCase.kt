package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.repository.SupplierRepository

class SeedSuppliersUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke() = repository.seedDefaultSuppliers()
}
