package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.ReceiptOutcome
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.repository.ReceiptRepository

/**
 * Responsibility: Logic for associating a floating receipt with a specific client.
 */
class LinkReceiptToClientUseCase(
    private val repository: ReceiptRepository
) {
    suspend operator fun invoke(receipt: ReceiptItem, clientName: String): ReceiptOutcome {
        return repository.updateItem(receipt.copy(
            clientName = clientName,
            isBilled = true
        ))
    }
}
