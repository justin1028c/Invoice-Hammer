package com.fordham.toolbelt.domain.usecase.stripe

import com.fordham.toolbelt.data.remote.StripeConnectOnboardOutcome
import com.fordham.toolbelt.data.remote.StripeConnectStatusOutcome
import com.fordham.toolbelt.data.remote.StripePaymentBackendClient
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.stripe.StripeConnectOnboardingOutcome
import com.fordham.toolbelt.domain.model.stripe.StripeConnectSetupState
import com.fordham.toolbelt.domain.model.stripe.StripeOnboardingUrl
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.StripeIntegrationRepository

class RefreshStripeConnectStatusUseCase(
    private val stripeIntegration: StripeIntegrationRepository,
    private val stripeBackend: StripePaymentBackendClient,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): StripeConnectSetupState {
        if (!stripeIntegration.isPaymentSheetReady) {
            return StripeConnectSetupState.BackendDisabled
        }
        val userId = authRepository.currentUser.value?.id?.value
            ?: return StripeConnectSetupState.SignInRequired

        return when (val outcome = stripeBackend.fetchConnectStatus(userId)) {
            StripeConnectStatusOutcome.NotConfigured -> StripeConnectSetupState.BackendDisabled
            is StripeConnectStatusOutcome.Failure -> StripeConnectSetupState.Error(outcome.error)
            is StripeConnectStatusOutcome.Success -> {
                val status = outcome.status
                when {
                    status.chargesEnabled && !status.accountId.isNullOrBlank() ->
                        StripeConnectSetupState.Active(
                            accountId = status.accountId,
                            payoutsEnabled = status.payoutsEnabled
                        )
                    else ->
                        StripeConnectSetupState.Incomplete(
                            accountId = status.accountId,
                            payoutsEnabled = status.payoutsEnabled
                        )
                }
            }
        }
    }
}

class StartStripeConnectOnboardingUseCase(
    private val stripeIntegration: StripeIntegrationRepository,
    private val stripeBackend: StripePaymentBackendClient,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): StripeConnectOnboardingOutcome {
        if (!stripeIntegration.isPaymentSheetReady) {
            return StripeConnectOnboardingOutcome.BackendNotConfigured
        }
        val userId = authRepository.currentUser.value?.id?.value
            ?: return StripeConnectOnboardingOutcome.SignInRequired

        return when (val outcome = stripeBackend.startConnectOnboarding(userId)) {
            StripeConnectOnboardOutcome.NotConfigured ->
                StripeConnectOnboardingOutcome.BackendNotConfigured
            is StripeConnectOnboardOutcome.Failure ->
                StripeConnectOnboardingOutcome.Failure(outcome.error)
            is StripeConnectOnboardOutcome.Success -> {
                val url = outcome.response.onboardingUrl.trim()
                if (url.isBlank()) {
                    StripeConnectOnboardingOutcome.Failure(
                        FailureMessage("Stripe onboarding URL was empty.")
                    )
                } else {
                    StripeConnectOnboardingOutcome.Ready(StripeOnboardingUrl(url))
                }
            }
        }
    }
}
