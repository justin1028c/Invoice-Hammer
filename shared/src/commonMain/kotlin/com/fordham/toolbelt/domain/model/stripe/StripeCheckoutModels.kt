package com.fordham.toolbelt.domain.model.stripe

import com.fordham.toolbelt.domain.model.FailureMessage
import kotlin.jvm.JvmInline

@JvmInline
value class CheckoutUrl(val value: String) {
    init {
        require(value.startsWith("https://")) { "CheckoutUrl must use HTTPS." }
    }
}

sealed interface StripeCheckoutSessionOutcome {
    data class Success(val checkoutUrl: CheckoutUrl) : StripeCheckoutSessionOutcome
    data object NotConfigured : StripeCheckoutSessionOutcome
    data class Failure(val error: FailureMessage) : StripeCheckoutSessionOutcome
}
