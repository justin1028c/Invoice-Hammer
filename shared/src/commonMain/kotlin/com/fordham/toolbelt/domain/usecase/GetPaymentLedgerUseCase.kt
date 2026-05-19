package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.repository.PaymentRepository

class GetPaymentLedgerUseCase(
    repository: PaymentRepository
) {
    val ledger = repository.ledger
}
