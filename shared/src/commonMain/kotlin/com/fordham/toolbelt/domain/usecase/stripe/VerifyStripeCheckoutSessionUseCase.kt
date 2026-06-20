package com.fordham.toolbelt.domain.usecase.stripe

import com.fordham.toolbelt.data.remote.StripeCheckoutVerifyOutcome
import com.fordham.toolbelt.data.remote.StripePaymentBackendClient
import com.fordham.toolbelt.domain.model.FailureMessage

sealed interface VerifyStripeCheckoutOutcome {
    data class Verified(val invoiceId: String) : VerifyStripeCheckoutOutcome
    data object NotPaid : VerifyStripeCheckoutOutcome
    data object BackendNotConfigured : VerifyStripeCheckoutOutcome
    data class Failure(val error: FailureMessage) : VerifyStripeCheckoutOutcome
}

class VerifyStripeCheckoutSessionUseCase(
    private val stripeBackendClient: StripePaymentBackendClient
) {
    suspend operator fun invoke(
        sessionId: String,
        contractorUserId: String? = null
    ): VerifyStripeCheckoutOutcome {
        return when (
            val outcome = stripeBackendClient.verifyCheckoutSession(sessionId, contractorUserId)
        ) {
            StripeCheckoutVerifyOutcome.NotConfigured -> VerifyStripeCheckoutOutcome.BackendNotConfigured
            is StripeCheckoutVerifyOutcome.Failure -> VerifyStripeCheckoutOutcome.Failure(outcome.error)
            is StripeCheckoutVerifyOutcome.Success -> {
                val response = outcome.response
                if (response.paid) {
                    val invoiceId = response.invoiceId?.takeIf { it.isNotBlank() }
                    if (invoiceId != null) {
                        VerifyStripeCheckoutOutcome.Verified(invoiceId)
                    } else {
                        VerifyStripeCheckoutOutcome.NotPaid
                    }
                } else {
                    VerifyStripeCheckoutOutcome.NotPaid
                }
            }
        }
    }
}
