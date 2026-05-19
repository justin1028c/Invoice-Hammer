package com.fordham.toolbelt.domain.model

// ---------------------------------------------------------------------------
// Payment domain models — no Android, no Ktor, no DTOs here.
// ---------------------------------------------------------------------------

enum class PaymentStatus {
    UNPAID,
    PENDING,
    DEPOSIT_PAID,
    MILESTONE_PAID,
    PAID_IN_FULL,
    FAILED,
    UNKNOWN
}

data class ContractorBalance(
    val xlmBalance: String,
    val usdEstimate: String,
    val accountId: String,
    val isActive: Boolean
)

data class PaymentTransaction(
    val id: String,
    val invoiceId: String,
    val clientName: String,
    val amountUsd: Double,
    val amountXlm: String?,
    val status: PaymentStatus,
    val txHash: String?,
    val explorerUrl: String?,
    val createdAt: String,
    val type: PaymentType
)

enum class PaymentType { DEPOSIT, MILESTONE, FULL, UNKNOWN }

data class PaymentRequest(
    val invoiceId: String,
    val contractorUserId: String,
    val clientName: String,
    val amountUsd: Double,
    val depositAmountUsd: Double?,
    val description: String,
    val isTestnet: Boolean = true
)

data class MilestonePayment(
    val id: String,
    val paymentId: String,
    val description: String,
    val amountUsd: Double,
    val isReleased: Boolean,
    val releasedAt: String?
)

data class ActivePayment(
    val paymentId: String,
    val invoiceId: String,
    val status: PaymentStatus,
    val paymentLinkUrl: String?,
    val qrCodeUrl: String?,
    val milestones: List<MilestonePayment>
)
