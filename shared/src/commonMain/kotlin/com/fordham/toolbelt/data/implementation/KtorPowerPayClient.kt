package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.CreatePaymentRequestDto
import com.fordham.toolbelt.data.remote.CreatePaymentResponseDto
import com.fordham.toolbelt.data.remote.PaymentStatusResponseDto
import com.fordham.toolbelt.data.remote.PowerPayApiVariant
import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayClientOutcome
import com.fordham.toolbelt.data.remote.PowerPayConfig
import com.fordham.toolbelt.data.remote.PowerPayCreatePaymentRequestDto
import com.fordham.toolbelt.data.remote.PowerPayEventsPollResponseDto
import com.fordham.toolbelt.data.remote.PowerPayPaymentResponseDto
import com.fordham.toolbelt.data.remote.PowerPayWebhookEventDto
import com.fordham.toolbelt.data.remote.TransactionListResponseDto
import com.fordham.toolbelt.domain.model.FailureMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class KtorPowerPayClient(
    private val httpClient: HttpClient,
    private val config: PowerPayConfig
) : PowerPayClient {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override suspend fun createInvoicePayment(
        request: PowerPayCreatePaymentRequestDto
    ): PowerPayClientOutcome<PowerPayPaymentResponseDto> {
        if (!config.isConfigured) {
            return PowerPayClientOutcome.Failure(FailureMessage("PowerPay is not configured (baseUrl + appId required)."))
        }

        return when (config.apiVariant) {
            PowerPayApiVariant.SdkV1 -> postJson("/v1/invoice-payments", request)
            PowerPayApiVariant.RelayV1 -> createViaRelay(request)
        }
    }

    override suspend fun getPaymentStatus(paymentId: String): PowerPayClientOutcome<PowerPayPaymentResponseDto> {
        if (!config.isConfigured) {
            return PowerPayClientOutcome.Failure(FailureMessage("PowerPay is not configured."))
        }

        return when (config.apiVariant) {
            PowerPayApiVariant.SdkV1 -> getJson("/v1/payments/$paymentId")
            PowerPayApiVariant.RelayV1 -> getRelayStatus(paymentId)
        }
    }

    override suspend fun getTransactionHistory(): PowerPayClientOutcome<List<PowerPayPaymentResponseDto>> {
        if (!config.isConfigured) {
            return PowerPayClientOutcome.Failure(FailureMessage("PowerPay is not configured."))
        }

        return when (config.apiVariant) {
            PowerPayApiVariant.SdkV1 -> getSdkTransactions()
            PowerPayApiVariant.RelayV1 -> getRelayTransactions()
        }
    }

    override suspend fun pollWebhookEvents(
        sinceEpochSeconds: Long
    ): PowerPayClientOutcome<List<PowerPayWebhookEventDto>> {
        if (!config.isConfigured) {
            return PowerPayClientOutcome.Failure(FailureMessage("PowerPay is not configured."))
        }
        if (config.apiVariant == PowerPayApiVariant.RelayV1) {
            return PowerPayClientOutcome.Success(emptyList())
        }

        return try {
            val response = httpClient.get("${config.baseUrl.trimEnd('/')}/v1/events") {
                applyPowerPayHeaders()
                parameter("since", sinceEpochSeconds)
                parameter("preset", config.preset)
            }
            val body = response.bodyAsText()
            if (response.status.isSuccess()) {
                val parsed = json.decodeFromString<PowerPayEventsPollResponseDto>(body)
                PowerPayClientOutcome.Success(parsed.events)
            } else {
                PowerPayClientOutcome.Failure(
                    FailureMessage("PowerPay events poll failed with HTTP ${response.status.value}. $body")
                )
            }
        } catch (e: Exception) {
            PowerPayClientOutcome.Failure(FailureMessage(e.message ?: "PowerPay events poll failed."))
        }
    }

    private suspend fun createViaRelay(
        request: PowerPayCreatePaymentRequestDto
    ): PowerPayClientOutcome<PowerPayPaymentResponseDto> {
        val relayBody = CreatePaymentRequestDto(
            appId = request.appId,
            invoiceId = request.invoiceId,
            contractorUserId = request.contractorUserId,
            clientName = request.clientName,
            amountUsd = request.amountUsd,
            depositAmountUsd = if (request.requestType == "deposit") request.amountUsd else null,
            description = request.description,
            isTestnet = config.isTestnet
        )
        return try {
            val response = httpClient.post("${config.baseUrl.trimEnd('/')}/api/v1/payments") {
                applyPowerPayHeaders()
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(json.encodeToString(relayBody))
            }
            val body = response.bodyAsText()
            if (response.status.isSuccess()) {
                val created = json.decodeFromString<CreatePaymentResponseDto>(body)
                PowerPayClientOutcome.Success(PowerPayResponseMapper.fromCreate(created, request))
            } else {
                PowerPayClientOutcome.Failure(
                    FailureMessage("PowerPay create payment failed with HTTP ${response.status.value}. $body")
                )
            }
        } catch (e: Exception) {
            PowerPayClientOutcome.Failure(FailureMessage(e.message ?: "PowerPay create payment failed."))
        }
    }

    private suspend fun getRelayStatus(paymentId: String): PowerPayClientOutcome<PowerPayPaymentResponseDto> {
        return try {
            val response = httpClient.get("${config.baseUrl.trimEnd('/')}/api/v1/payments/$paymentId") {
                applyPowerPayHeaders()
            }
            val body = response.bodyAsText()
            if (response.status.isSuccess()) {
                val status = json.decodeFromString<PaymentStatusResponseDto>(body)
                PowerPayClientOutcome.Success(PowerPayResponseMapper.fromStatus(status))
            } else {
                PowerPayClientOutcome.Failure(
                    FailureMessage("PowerPay status failed with HTTP ${response.status.value}. $body")
                )
            }
        } catch (e: Exception) {
            PowerPayClientOutcome.Failure(FailureMessage(e.message ?: "PowerPay status failed."))
        }
    }

    private suspend fun getSdkTransactions(): PowerPayClientOutcome<List<PowerPayPaymentResponseDto>> {
        return try {
            val response = httpClient.get("${config.baseUrl.trimEnd('/')}/v1/transactions") {
                applyPowerPayHeaders()
            }
            val body = response.bodyAsText()
            if (response.status.isSuccess()) {
                PowerPayClientOutcome.Success(decodeTransactionList(body))
            } else {
                PowerPayClientOutcome.Failure(
                    FailureMessage("PowerPay transaction history failed with HTTP ${response.status.value}. $body")
                )
            }
        } catch (e: Exception) {
            PowerPayClientOutcome.Failure(FailureMessage(e.message ?: "PowerPay transaction history failed."))
        }
    }

    private suspend fun getRelayTransactions(): PowerPayClientOutcome<List<PowerPayPaymentResponseDto>> {
        return try {
            val response = httpClient.get("${config.baseUrl.trimEnd('/')}/api/v1/transactions") {
                applyPowerPayHeaders()
            }
            val body = response.bodyAsText()
            if (response.status.isSuccess()) {
                val list = json.decodeFromString<TransactionListResponseDto>(body)
                PowerPayClientOutcome.Success(list.transactions.map { PowerPayResponseMapper.fromTransaction(it) })
            } else {
                PowerPayClientOutcome.Failure(
                    FailureMessage("PowerPay transaction history failed with HTTP ${response.status.value}. $body")
                )
            }
        } catch (e: Exception) {
            PowerPayClientOutcome.Failure(FailureMessage(e.message ?: "PowerPay transaction history failed."))
        }
    }

    private suspend inline fun <reified T> postJson(path: String, body: T): PowerPayClientOutcome<PowerPayPaymentResponseDto> {
        return try {
            val response = httpClient.post("${config.baseUrl.trimEnd('/')}$path") {
                applyPowerPayHeaders()
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(json.encodeToString(body))
            }
            decodePaymentResponse(response.bodyAsText(), response.status.value)
        } catch (e: Exception) {
            PowerPayClientOutcome.Failure(FailureMessage(e.message ?: "PowerPay request failed."))
        }
    }

    private suspend fun getJson(path: String): PowerPayClientOutcome<PowerPayPaymentResponseDto> {
        return try {
            val response = httpClient.get("${config.baseUrl.trimEnd('/')}$path") {
                applyPowerPayHeaders()
            }
            decodePaymentResponse(response.bodyAsText(), response.status.value)
        } catch (e: Exception) {
            PowerPayClientOutcome.Failure(FailureMessage(e.message ?: "PowerPay request failed."))
        }
    }

    private fun decodePaymentResponse(body: String, statusCode: Int): PowerPayClientOutcome<PowerPayPaymentResponseDto> {
        if (statusCode !in 200..299) {
            return PowerPayClientOutcome.Failure(FailureMessage("PowerPay request failed with HTTP $statusCode. $body"))
        }
        return try {
            PowerPayClientOutcome.Success(json.decodeFromString<PowerPayPaymentResponseDto>(body))
        } catch (_: Exception) {
            try {
                val status = json.decodeFromString<PaymentStatusResponseDto>(body)
                PowerPayClientOutcome.Success(PowerPayResponseMapper.fromStatus(status))
            } catch (e: Exception) {
                PowerPayClientOutcome.Failure(FailureMessage(e.message ?: "Unrecognized PowerPay response."))
            }
        }
    }

    private fun decodeTransactionList(body: String): List<PowerPayPaymentResponseDto> {
        return try {
            json.decodeFromString<List<PowerPayPaymentResponseDto>>(body)
        } catch (_: Exception) {
            json.decodeFromString<TransactionListResponseDto>(body).transactions.map { PowerPayResponseMapper.fromTransaction(it) }
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyPowerPayHeaders() {
        accept(ContentType.Application.Json)
        header("X-PowerPay-App-Id", config.appId)
        header("X-PowerPay-Preset", config.preset)
        header("X-PowerPay-Environment", config.environment.wireName)
        config.publicApiKey?.takeIf { it.isNotBlank() }?.let { key ->
            header(HttpHeaders.Authorization, "Bearer $key")
        }
    }
}
