package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayClientOutcome
import com.fordham.toolbelt.data.remote.PowerPayConfig
import com.fordham.toolbelt.data.remote.PowerPayCreatePaymentRequestDto
import com.fordham.toolbelt.data.remote.PowerPayPaymentResponseDto
import com.fordham.toolbelt.domain.model.FailureMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.get
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

    override suspend fun createInvoicePayment(request: PowerPayCreatePaymentRequestDto): PowerPayClientOutcome<PowerPayPaymentResponseDto> {
        if (!config.isConfigured) {
            return PowerPayClientOutcome.Failure(FailureMessage("PowerPay backend is not configured yet."))
        }

        return try {
            val response = httpClient.post("${config.baseUrl.trimEnd('/')}/v1/invoice-payments") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                config.apiKey?.takeIf { it.isNotBlank() }?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(json.encodeToString(request))
            }

            val body = response.bodyAsText()
            if (response.status.isSuccess()) {
                PowerPayClientOutcome.Success(json.decodeFromString<PowerPayPaymentResponseDto>(body))
            } else {
                PowerPayClientOutcome.Failure(FailureMessage("PowerPay create payment failed with HTTP ${response.status.value}. $body"))
            }
        } catch (e: Exception) {
            PowerPayClientOutcome.Failure(FailureMessage(e.message ?: "PowerPay create payment failed."))
        }
    }

    override suspend fun getTransactionHistory(): PowerPayClientOutcome<List<PowerPayPaymentResponseDto>> {
        if (!config.isConfigured) {
            return PowerPayClientOutcome.Failure(FailureMessage("PowerPay backend is not configured yet."))
        }

        return try {
            val response = httpClient.get("${config.baseUrl.trimEnd('/')}/v1/transactions") {
                accept(ContentType.Application.Json)
                config.apiKey?.takeIf { it.isNotBlank() }?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }

            val body = response.bodyAsText()
            if (response.status.isSuccess()) {
                PowerPayClientOutcome.Success(json.decodeFromString<List<PowerPayPaymentResponseDto>>(body))
            } else {
                PowerPayClientOutcome.Failure(FailureMessage("PowerPay transaction history failed with HTTP ${response.status.value}. $body"))
            }
        } catch (e: Exception) {
            PowerPayClientOutcome.Failure(FailureMessage(e.message ?: "PowerPay transaction history failed."))
        }
    }
}
