package com.fordham.toolbelt.data.remote

import com.fordham.toolbelt.domain.model.FailureMessage
import kotlinx.serialization.Serializable

interface PowerPayClient {
    suspend fun createInvoicePayment(request: PowerPayCreatePaymentRequestDto): PowerPayClientOutcome<PowerPayPaymentResponseDto>
    suspend fun getPaymentStatus(paymentId: String): PowerPayClientOutcome<PowerPayPaymentResponseDto>
    suspend fun getTransactionHistory(): PowerPayClientOutcome<List<PowerPayPaymentResponseDto>>
    suspend fun pollWebhookEvents(sinceEpochSeconds: Long): PowerPayClientOutcome<List<PowerPayWebhookEventDto>>
}

sealed interface PowerPayClientOutcome<out T> {
    data class Success<T>(val value: T) : PowerPayClientOutcome<T>
    data class Failure(val error: FailureMessage) : PowerPayClientOutcome<Nothing>
}

@Serializable
data class PowerPayCreatePaymentRequestDto(
    val appId: String,
    val contractorUserId: String,
    val invoiceId: String,
    val clientName: String,
    val amountUsd: Double,
    val requestType: String,
    val provider: String,
    val description: String,
    val preset: String = PowerPayConfig.PRESET_DIGITAL_TOOL_BELT,
    val environment: String = PowerPayEnvironment.Sandbox.wireName,
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
    val receiptUrl: String? = null,
    val explorerUrl: String? = null,
    val createdAtMillis: Long
)

@Serializable
data class PowerPayEventsPollResponseDto(
    val events: List<PowerPayWebhookEventDto> = emptyList()
)
