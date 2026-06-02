package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.StripeConfig
import com.fordham.toolbelt.data.remote.StripeCreatePaymentIntentRequest
import com.fordham.toolbelt.data.remote.StripePaymentBackendClient
import com.fordham.toolbelt.data.remote.StripePaymentIntentOutcome
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.stripe.CreateStripePaymentIntentCommand
import com.fordham.toolbelt.domain.model.stripe.CreateStripePaymentIntentOutcome
import com.fordham.toolbelt.domain.repository.StripePaymentIntentRepository

class StripePaymentIntentRepositoryImpl(
    private val backendClient: StripePaymentBackendClient,
    private val config: StripeConfig
) : StripePaymentIntentRepository {

    override suspend fun createPaymentIntent(
        command: CreateStripePaymentIntentCommand
    ): CreateStripePaymentIntentOutcome {
        val outcome = backendClient.createPaymentIntent(
            StripeCreatePaymentIntentRequest(
                amountCents = command.amountCents,
                invoiceId = command.invoiceId.value,
                contractorUserId = command.contractorUserId,
                clientName = command.clientName,
                requestType = command.requestType.toWireName(),
                paymentProvider = command.channel.wireName,
                applicationFeeBps = config.applicationFeeBps
            )
        )
        return when (outcome) {
            StripePaymentIntentOutcome.NotConfigured -> CreateStripePaymentIntentOutcome.BackendNotConfigured
            is StripePaymentIntentOutcome.Failure ->
                CreateStripePaymentIntentOutcome.Failure(outcome.error)
            is StripePaymentIntentOutcome.Success ->
                CreateStripePaymentIntentOutcome.Ready(
                    clientSecret = outcome.response.clientSecret,
                    paymentIntentId = outcome.response.paymentIntentId,
                    stripeAccountId = outcome.response.stripeAccountId
                )
        }
    }

    private fun PaymentRequestType.toWireName(): String = when (this) {
        PaymentRequestType.Deposit -> "deposit"
        PaymentRequestType.FullBalance -> "full_balance"
    }
}
