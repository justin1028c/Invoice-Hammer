package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.stripe.CreateStripePaymentIntentCommand
import com.fordham.toolbelt.domain.model.stripe.CreateStripePaymentIntentOutcome

interface StripePaymentIntentRepository {
    suspend fun createPaymentIntent(
        command: CreateStripePaymentIntentCommand
    ): CreateStripePaymentIntentOutcome
}
