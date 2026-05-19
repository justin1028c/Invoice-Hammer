package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.domain.repository.SupplierRepository

class LogSupplierPurchaseUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(id: SupplierId, amount: MoneyAmount) = repository.logPurchase(id, amount)
}
