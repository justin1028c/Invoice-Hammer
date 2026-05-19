package com.fordham.toolbelt.data.remote

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// PowerPay API DTOs — data layer ONLY. Never exposed to domain or UI.
// ---------------------------------------------------------------------------

@Serializable
data class CreatePaymentRequestDto(
    val appId: String,
    val invoiceId: String,
    val contractorUserId: String,
    val clientName: String,
    val amountUsd: Double,
    val depositAmountUsd: Double? = null,
    val description: String,
    val isTestnet: Boolean = true
)

@Serializable
data class CreatePaymentResponseDto(
    val paymentId: String,
    val status: String,
    val paymentLinkUrl: String? = null,
    val qrCodeUrl: String? = null
)

@Serializable
data class PaymentStatusResponseDto(
    val paymentId: String,
    val invoiceId: String,
    val status: String,
    val amountUsd: Double? = null,
    val amountXlm: String? = null,
    val txHash: String? = null,
    val explorerUrl: String? = null,
    val milestones: List<MilestoneDto> = emptyList()
)

@Serializable
data class MilestoneDto(
    val id: String,
    val paymentId: String,
    val description: String,
    val amountUsd: Double,
    val isReleased: Boolean,
    val releasedAt: String? = null
)

@Serializable
data class ReleaseMilestoneRequestDto(
    val paymentId: String,
    val milestoneId: String
)

@Serializable
data class ReleaseMilestoneResponseDto(
    val success: Boolean,
    val txHash: String? = null,
    val explorerUrl: String? = null
)

@Serializable
data class BalanceResponseDto(
    val accountId: String,
    val xlmBalance: String,
    val usdEstimate: String,
    val isActive: Boolean
)

@Serializable
data class TransactionListResponseDto(
    val transactions: List<TransactionDto>
)

@Serializable
data class TransactionDto(
    val id: String,
    val invoiceId: String,
    val clientName: String,
    val amountUsd: Double? = null,
    val amountXlm: String? = null,
    val status: String,
    val txHash: String? = null,
    val explorerUrl: String? = null,
    val createdAt: String,
    val type: String = "UNKNOWN"
)

@Serializable
data class RequestDepositRequestDto(
    val paymentId: String,
    val depositAmountUsd: Double
)

@Serializable
data class PowerPayErrorDto(
    val code: String,
    val message: String
)
