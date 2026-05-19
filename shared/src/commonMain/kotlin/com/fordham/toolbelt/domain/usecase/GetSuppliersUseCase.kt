package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.SupplierListOutcome
import com.fordham.toolbelt.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.Flow

class GetSuppliersUseCase(
    private val repository: SupplierRepository
) {
    operator fun invoke(): Flow<SupplierListOutcome> = repository.getVisibleSuppliers()
}
