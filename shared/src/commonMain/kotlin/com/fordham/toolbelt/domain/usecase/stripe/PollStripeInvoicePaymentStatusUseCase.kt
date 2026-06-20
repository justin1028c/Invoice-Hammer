package com.fordham.toolbelt.domain.usecase.stripe

import com.fordham.toolbelt.data.remote.StripeInvoicePaymentStatusOutcome
import com.fordham.toolbelt.data.remote.StripePaymentBackendClient
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first

class PollStripeInvoicePaymentStatusUseCase(
    private val stripeBackendClient: StripePaymentBackendClient,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(invoiceIds: List<InvoiceId>): Set<InvoiceId> {
        if (invoiceIds.isEmpty()) return emptySet()
        val contractorUserId = authRepository.currentUser.first()?.id?.value?.takeIf { it.isNotBlank() }
            ?: return emptySet()

        val paid = linkedSetOf<InvoiceId>()
        for (invoiceId in invoiceIds) {
            when (
                val outcome = stripeBackendClient.fetchInvoicePaymentStatus(
                    invoiceId.value,
                    contractorUserId
                )
            ) {
                is StripeInvoicePaymentStatusOutcome.Success -> {
                    if (outcome.response.paid) {
                        paid += invoiceId
                    }
                }
                StripeInvoicePaymentStatusOutcome.NotConfigured,
                is StripeInvoicePaymentStatusOutcome.Failure -> Unit
            }
        }
        return paid
    }
}
