package com.fordham.toolbelt.domain.model.stripe

import com.fordham.toolbelt.domain.model.FailureMessage
import kotlin.jvm.JvmInline

@JvmInline
value class StripeOnboardingUrl(val value: String)

/** Stripe Connect readiness for the signed-in contractor (Settings + payments). */
sealed interface StripeConnectSetupState {
    /** `stripe.payment.backend.url` or publishable key missing. */
    data object BackendDisabled : StripeConnectSetupState

    data object SignInRequired : StripeConnectSetupState
    data object Loading : StripeConnectSetupState

    data class Incomplete(
        val accountId: String?,
        val payoutsEnabled: Boolean
    ) : StripeConnectSetupState

    data class Active(
        val accountId: String,
        val payoutsEnabled: Boolean
    ) : StripeConnectSetupState

    data class Error(val message: FailureMessage) : StripeConnectSetupState
}

sealed interface StripeConnectOnboardingOutcome {
    data class Ready(val url: StripeOnboardingUrl) : StripeConnectOnboardingOutcome
    data object BackendNotConfigured : StripeConnectOnboardingOutcome
    data object SignInRequired : StripeConnectOnboardingOutcome
    data class Failure(val error: FailureMessage) : StripeConnectOnboardingOutcome
}
