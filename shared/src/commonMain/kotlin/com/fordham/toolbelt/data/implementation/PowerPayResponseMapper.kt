package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.CreatePaymentResponseDto
import com.fordham.toolbelt.data.remote.PaymentStatusResponseDto
import com.fordham.toolbelt.data.remote.PowerPayCreatePaymentRequestDto
import com.fordham.toolbelt.data.remote.PowerPayPaymentResponseDto
import com.fordham.toolbelt.data.remote.TransactionDto
import kotlinx.datetime.Clock

internal object PowerPayResponseMapper {
    fun fromCreate(
        created: CreatePaymentResponseDto,
        request: PowerPayCreatePaymentRequestDto
    ): PowerPayPaymentResponseDto {
        return PowerPayPaymentResponseDto(
            paymentId = created.paymentId,
            invoiceId = request.invoiceId,
            clientName = request.clientName,
            amountUsd = request.amountUsd,
            requestType = request.requestType,
            provider = request.provider,
            status = created.status,
            paymentLinkUrl = created.paymentLinkUrl.orEmpty(),
            assetCode = "USDC",
            transactionHash = null,
            receiptUrl = null,
            explorerUrl = null,
            createdAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    fun fromStatus(status: PaymentStatusResponseDto): PowerPayPaymentResponseDto {
        return PowerPayPaymentResponseDto(
            paymentId = status.paymentId,
            invoiceId = status.invoiceId,
            clientName = "",
            amountUsd = status.amountUsd ?: 0.0,
            requestType = "full_balance",
            provider = "stellar_usdc",
            status = status.status,
            paymentLinkUrl = "",
            assetCode = "USDC",
            transactionHash = status.txHash,
            receiptUrl = null,
            explorerUrl = status.explorerUrl,
            createdAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    fun fromTransaction(transaction: TransactionDto): PowerPayPaymentResponseDto {
        return PowerPayPaymentResponseDto(
            paymentId = transaction.id,
            invoiceId = transaction.invoiceId,
            clientName = transaction.clientName,
            amountUsd = transaction.amountUsd ?: 0.0,
            requestType = when (transaction.type.uppercase()) {
                "DEPOSIT" -> "deposit"
                else -> "full_balance"
            },
            provider = "stellar_usdc",
            status = transaction.status,
            paymentLinkUrl = "",
            assetCode = "USDC",
            transactionHash = transaction.txHash,
            receiptUrl = null,
            explorerUrl = transaction.explorerUrl,
            createdAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }
}
