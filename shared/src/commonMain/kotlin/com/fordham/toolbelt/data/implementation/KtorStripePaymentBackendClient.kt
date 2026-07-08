package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.StripeConfig
import com.fordham.toolbelt.data.remote.StripeConnectAccountStatusResponse
import com.fordham.toolbelt.data.remote.StripeConnectOnboardOutcome
import com.fordham.toolbelt.data.remote.StripeConnectOnboardRequest
import com.fordham.toolbelt.data.remote.StripeConnectOnboardResponse
import com.fordham.toolbelt.data.remote.StripeConnectStatusOutcome
import com.fordham.toolbelt.data.remote.StripeCreatePaymentIntentRequest
import com.fordham.toolbelt.data.remote.StripeCreatePaymentIntentResponse
import com.fordham.toolbelt.data.remote.StripePaymentBackendClient
import com.fordham.toolbelt.data.remote.StripePaymentIntentOutcome
import com.fordham.toolbelt.data.remote.StripeCreateCheckoutSessionRequest
import com.fordham.toolbelt.data.remote.StripeCreateCheckoutSessionResponse
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.data.remote.StripeCheckoutLinkOutcome
import com.fordham.toolbelt.data.remote.StripeCheckoutLinkResponse
import com.fordham.toolbelt.data.remote.StripeCheckoutVerifyOutcome
import com.fordham.toolbelt.data.remote.StripeCheckoutVerifyResponse
import com.fordham.toolbelt.data.remote.StripeInvoicePaymentStatusOutcome
import com.fordham.toolbelt.data.remote.StripeInvoicePaymentStatusResponse
import com.fordham.toolbelt.domain.model.stripe.CheckoutUrl
import com.fordham.toolbelt.domain.model.stripe.StripeCheckoutSessionOutcome
import com.fordham.toolbelt.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class KtorStripePaymentBackendClient(
    private val httpClient: HttpClient,
    private val config: StripeConfig,
    private val authRepository: AuthRepository
) : StripePaymentBackendClient {

    private suspend fun HttpRequestBuilder.applyBackendAuth() {
        val token = authRepository.getBackendIdToken()
            ?: error("Sign in is required for Stripe operations.")
        header("Authorization", "Bearer ${token.value}")
    }

    override suspend fun createPaymentIntent(
        request: StripeCreatePaymentIntentRequest
    ): StripePaymentIntentOutcome {
        if (!config.isBackendConfigured) return StripePaymentIntentOutcome.NotConfigured
        return runCatching {
            val response = httpClient.post("${config.paymentBackendBaseUrl.trimEnd('/')}/v1/payments/intent") {
                applyBackendAuth()
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (!response.status.isSuccess()) {
                val details = mapStripeBackendFailure(response, "Payment backend error")
                return StripePaymentIntentOutcome.Failure(details.error, details.actionUrl)
            }
            StripePaymentIntentOutcome.Success(response.body<StripeCreatePaymentIntentResponse>())
        }.getOrElse {
            StripePaymentIntentOutcome.Failure(FailureMessage(it.message ?: "Payment backend unreachable."))
        }
    }

    override suspend fun fetchConnectStatus(contractorUserId: String): StripeConnectStatusOutcome {
        if (!config.isBackendConfigured) return StripeConnectStatusOutcome.NotConfigured
        return runCatching {
            val response = httpClient.get(
                "${config.paymentBackendBaseUrl.trimEnd('/')}/v1/connect/status?userId=$contractorUserId"
            ) {
                applyBackendAuth()
            }
            if (!response.status.isSuccess()) {
                val details = mapStripeBackendFailure(response, "Connect status error")
                return StripeConnectStatusOutcome.Failure(details.error)
            }
            StripeConnectStatusOutcome.Success(response.body<StripeConnectAccountStatusResponse>())
        }.getOrElse {
            StripeConnectStatusOutcome.Failure(FailureMessage(it.message ?: "Connect status unreachable."))
        }
    }

    override suspend fun startConnectOnboarding(
        contractorUserId: String
    ): StripeConnectOnboardOutcome {
        if (!config.isBackendConfigured) return StripeConnectOnboardOutcome.NotConfigured
        return runCatching {
            val response = httpClient.post(
                "${config.paymentBackendBaseUrl.trimEnd('/')}/v1/connect/onboard"
            ) {
                applyBackendAuth()
                contentType(ContentType.Application.Json)
                setBody(StripeConnectOnboardRequest(contractorUserId = contractorUserId))
            }
            if (!response.status.isSuccess()) {
                val details = mapStripeBackendFailure(response, "Connect onboarding error")
                return StripeConnectOnboardOutcome.Failure(details.error)
            }
            StripeConnectOnboardOutcome.Success(response.body<StripeConnectOnboardResponse>())
        }.getOrElse {
            StripeConnectOnboardOutcome.Failure(
                FailureMessage(it.message ?: "Connect onboarding unreachable.")
            )
        }
    }

    override suspend fun createCheckoutSession(
        invoiceId: String,
        amountInCents: Long,
        contractorUserId: String
    ): StripeCheckoutSessionOutcome {
        if (!config.isBackendConfigured) return StripeCheckoutSessionOutcome.NotConfigured
        return runCatching {
            val response = httpClient.post("${config.paymentBackendBaseUrl.trimEnd('/')}/v1/payments/checkout-session") {
                applyBackendAuth()
                contentType(ContentType.Application.Json)
                setBody(
                    StripeCreateCheckoutSessionRequest(
                        amountCents = amountInCents,
                        invoiceId = invoiceId,
                        contractorUserId = contractorUserId
                    )
                )
            }
            if (!response.status.isSuccess()) {
                val details = mapStripeBackendFailure(response, "Checkout session error")
                return StripeCheckoutSessionOutcome.Failure(details.error)
            }
            val body = response.body<StripeCreateCheckoutSessionResponse>()
            StripeCheckoutSessionOutcome.Success(CheckoutUrl(body.checkoutUrl))
        }.getOrElse {
            StripeCheckoutSessionOutcome.Failure(
                FailureMessage(it.message ?: "Checkout backend unreachable.")
            )
        }
    }

    override suspend fun verifyCheckoutSession(
        sessionId: String,
        contractorUserId: String?
    ): StripeCheckoutVerifyOutcome {
        if (!config.isBackendConfigured) return StripeCheckoutVerifyOutcome.NotConfigured
        return runCatching {
            val contractorQuery = contractorUserId
                ?.takeIf { it.isNotBlank() }
                ?.let { "&contractorUserId=$it" }
                .orEmpty()
            val response = httpClient.get(
                "${config.paymentBackendBaseUrl.trimEnd('/')}/v1/payments/verify" +
                    "?session_id=$sessionId$contractorQuery"
            ) {
                applyBackendAuth()
            }
            if (!response.status.isSuccess()) {
                val details = mapStripeBackendFailure(response, "Checkout verify error")
                return StripeCheckoutVerifyOutcome.Failure(details.error)
            }
            StripeCheckoutVerifyOutcome.Success(response.body<StripeCheckoutVerifyResponse>())
        }.getOrElse {
            StripeCheckoutVerifyOutcome.Failure(
                FailureMessage(it.message ?: "Checkout verify unreachable.")
            )
        }
    }

    override suspend fun fetchInvoicePaymentStatus(
        invoiceId: String,
        contractorUserId: String
    ): StripeInvoicePaymentStatusOutcome {
        if (!config.isBackendConfigured) return StripeInvoicePaymentStatusOutcome.NotConfigured
        return runCatching {
            val response = httpClient.get(
                "${config.paymentBackendBaseUrl.trimEnd('/')}/v1/payments/invoice-status" +
                    "?invoiceId=$invoiceId&contractorUserId=$contractorUserId"
            ) {
                applyBackendAuth()
            }
            if (!response.status.isSuccess()) {
                val details = mapStripeBackendFailure(response, "Invoice payment status error")
                return StripeInvoicePaymentStatusOutcome.Failure(details.error)
            }
            StripeInvoicePaymentStatusOutcome.Success(
                response.body<StripeInvoicePaymentStatusResponse>()
            )
        }.getOrElse {
            StripeInvoicePaymentStatusOutcome.Failure(
                FailureMessage(it.message ?: "Invoice payment status unreachable.")
            )
        }
    }

    override suspend fun resolveCheckoutLink(
        sessionId: String,
        contractorUserId: String
    ): StripeCheckoutLinkOutcome {
        if (!config.isBackendConfigured) return StripeCheckoutLinkOutcome.NotConfigured
        return runCatching {
            val response = httpClient.get(
                "${config.paymentBackendBaseUrl.trimEnd('/')}/v1/payments/checkout-link" +
                    "?session_id=$sessionId&contractorUserId=$contractorUserId"
            ) {
                applyBackendAuth()
            }
            if (!response.status.isSuccess()) {
                val details = mapStripeBackendFailure(response, "Checkout link error")
                return StripeCheckoutLinkOutcome.Failure(details.error)
            }
            StripeCheckoutLinkOutcome.Success(response.body<StripeCheckoutLinkResponse>())
        }.getOrElse {
            StripeCheckoutLinkOutcome.Failure(
                FailureMessage(it.message ?: "Checkout link unreachable.")
            )
        }
    }
}
