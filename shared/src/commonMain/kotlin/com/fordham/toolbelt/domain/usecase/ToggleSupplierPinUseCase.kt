package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.domain.repository.SupplierRepository

class ToggleSupplierPinUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(id: SupplierId, isPinned: Boolean) = repository.togglePin(id, isPinned)
}
