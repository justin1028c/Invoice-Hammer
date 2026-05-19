package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestOutcome
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.repository.PaymentRepository

class CreatePaymentRequestUseCase(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(invoice: Invoice, type: PaymentRequestType, provider: PaymentProviderType): PaymentRequestOutcome {
        return repository.createPaymentRequest(invoice, type, provider)
    }
}
