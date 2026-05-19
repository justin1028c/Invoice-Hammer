package com.fordham.toolbelt.data.remote

import com.fordham.toolbelt.domain.model.FailureMessage
import kotlinx.serialization.Serializable

data class PowerPayConfig(
    val baseUrl: String,
    val apiKey: String? = null
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank()
}

interface PowerPayClient {
    suspend fun createInvoicePayment(request: PowerPayCreatePaymentRequestDto): PowerPayClientOutcome<PowerPayPaymentResponseDto>
    suspend fun getTransactionHistory(): PowerPayClientOutcome<List<PowerPayPaymentResponseDto>>
}

sealed interface PowerPayClientOutcome<out T> {
    data class Success<T>(val value: T) : PowerPayClientOutcome<T>
    data class Failure(val error: FailureMessage) : PowerPayClientOutcome<Nothing>
}

@Serializable
data class PowerPayCreatePaymentRequestDto(
    val invoiceId: String,
    val clientName: String,
    val amountUsd: Double,
    val requestType: String,
    val provider: String,
    val description: String,
    val appSource: String = "invoice_hammer"
)

@Serializable
data class PowerPayPaymentResponseDto(
    val paymentId: String,
    val invoiceId: String,
    val clientName: String,
    val amountUsd: Double,
    val requestType: String,
    val provider: String,
    val status: String,
    val paymentLinkUrl: String,
    val assetCode: String,
    val transactionHash: String? = null,
    val createdAtMillis: Long
)
