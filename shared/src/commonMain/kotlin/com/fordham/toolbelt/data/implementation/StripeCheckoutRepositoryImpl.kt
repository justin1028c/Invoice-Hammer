package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.StripePaymentBackendClient
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.stripe.StripeCheckoutSessionOutcome
import com.fordham.toolbelt.domain.repository.StripeCheckoutRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class StripeCheckoutRepositoryImpl(
    private val backendClient: StripePaymentBackendClient,
    private val ioDispatcher: CoroutineDispatcher
) : StripeCheckoutRepository {
    override suspend fun generateFallbackPaymentLink(
        invoiceId: InvoiceId,
        amountInCents: Long,
        contractorUserId: String
    ): StripeCheckoutSessionOutcome = withContext(ioDispatcher) {
        backendClient.createCheckoutSession(invoiceId.value, amountInCents, contractorUserId)
    }
}
