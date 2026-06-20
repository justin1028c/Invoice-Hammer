package com.fordham.toolbelt.domain.usecase.stripe

import com.fordham.toolbelt.data.remote.StripeCheckoutLinkOutcome
import com.fordham.toolbelt.data.remote.StripePaymentBackendClient
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first

class ResolveStripeCheckoutLinkUseCase(
    private val stripeBackendClient: StripePaymentBackendClient,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(sessionId: String): ResolveStripeCheckoutLinkOutcome {
        val contractorUserId = authRepository.currentUser.first()?.id?.value?.takeIf { it.isNotBlank() }
            ?: return ResolveStripeCheckoutLinkOutcome.Failure(
                FailureMessage("Sign in to refresh the payment link.")
            )
        return when (
            val outcome = stripeBackendClient.resolveCheckoutLink(sessionId, contractorUserId)
        ) {
            StripeCheckoutLinkOutcome.NotConfigured ->
                ResolveStripeCheckoutLinkOutcome.Failure(
                    FailureMessage("Stripe payment backend is not configured.")
                )
            is StripeCheckoutLinkOutcome.Failure ->
                ResolveStripeCheckoutLinkOutcome.Failure(outcome.error)
            is StripeCheckoutLinkOutcome.Success -> {
                val response = outcome.response
                ResolveStripeCheckoutLinkOutcome.Resolved(
                    checkoutUrl = response.checkoutUrl?.takeIf { it.isNotBlank() },
                    status = response.status,
                    paid = response.paid,
                    canPay = response.canPay
                )
            }
        }
    }
}

sealed interface ResolveStripeCheckoutLinkOutcome {
    data class Resolved(
        val checkoutUrl: String?,
        val status: String,
        val paid: Boolean,
        val canPay: Boolean
    ) : ResolveStripeCheckoutLinkOutcome

    data class Failure(val error: FailureMessage) : ResolveStripeCheckoutLinkOutcome
}
