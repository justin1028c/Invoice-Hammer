package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayClientOutcome
import com.fordham.toolbelt.data.remote.PowerPayCreatePaymentRequestDto
import com.fordham.toolbelt.data.remote.PowerPayPaymentResponseDto
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.InvoicePaymentStatus
import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.domain.model.PaymentLedgerOutcome
import com.fordham.toolbelt.domain.model.PaymentLinkUrl
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestId
import com.fordham.toolbelt.domain.model.PaymentRequestOutcome
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.StellarTransactionHash
import com.fordham.toolbelt.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PowerPayPaymentRepository(
    private val powerPayClient: PowerPayClient
) : PaymentRepository {
    private val requests = MutableStateFlow<PaymentLedgerOutcome>(PaymentLedgerOutcome.Success(emptyList()))

    override val ledger: Flow<PaymentLedgerOutcome> = requests.asStateFlow()

    override suspend fun createPaymentRequest(
        invoice: Invoice,
        type: PaymentRequestType,
        provider: PaymentProviderType
    ): PaymentRequestOutcome {
        if (invoice.totalAmount <= 0.0) {
            return PaymentRequestOutcome.Failure(FailureMessage("Invoice must have a positive total before requesting payment."))
        }

        val amount = when (type) {
            PaymentRequestType.Deposit -> invoice.depositAmount.takeIf { it > 0.0 } ?: invoice.totalAmount * DEFAULT_DEPOSIT_PERCENT
            PaymentRequestType.FullBalance -> invoice.totalAmount
        }

        val outcome = powerPayClient.createInvoicePayment(
            PowerPayCreatePaymentRequestDto(
                invoiceId = invoice.id.value,
                clientName = invoice.clientName,
                amountUsd = amount,
                requestType = type.wireName,
                provider = provider.wireName,
                description = "${type.descriptionLabel} for ${invoice.clientName}"
            )
        )

        return when (outcome) {
            is PowerPayClientOutcome.Success -> {
                val request = outcome.value.toDomain()
                val current = (requests.value as? PaymentLedgerOutcome.Success)?.requests.orEmpty()
                requests.value = PaymentLedgerOutcome.Success(
                    listOf(request) + current.filterNot {
                        it.invoiceId == request.invoiceId && it.type == request.type && it.provider == request.provider
                    }
                )
                PaymentRequestOutcome.Success(request)
            }
            is PowerPayClientOutcome.Failure -> PaymentRequestOutcome.Failure(outcome.error)
        }
    }

    override suspend fun refreshLedger(): PaymentLedgerOutcome {
        val outcome = powerPayClient.getTransactionHistory()
        val ledgerOutcome = when (outcome) {
            is PowerPayClientOutcome.Success -> PaymentLedgerOutcome.Success(outcome.value.map { it.toDomain() })
            is PowerPayClientOutcome.Failure -> PaymentLedgerOutcome.Failure(outcome.error)
        }
        requests.value = ledgerOutcome
        return ledgerOutcome
    }

    private fun PowerPayPaymentResponseDto.toDomain(): InvoicePaymentRequest {
        return InvoicePaymentRequest(
            id = PaymentRequestId(paymentId),
            invoiceId = InvoiceId(invoiceId),
            invoiceClientName = clientName,
            type = requestType.toPaymentRequestType(),
            provider = provider.toPaymentProviderType(),
            requestedAmount = MoneyAmount(amountUsd),
            status = status.toInvoicePaymentStatus(),
            paymentLink = PaymentLinkUrl(paymentLinkUrl),
            createdAtMillis = createdAtMillis,
            stellarTransactionHash = transactionHash?.let { StellarTransactionHash(it) },
            assetCode = assetCode
        )
    }

    private fun String.toPaymentRequestType(): PaymentRequestType = when (this) {
        "deposit" -> PaymentRequestType.Deposit
        "full_balance" -> PaymentRequestType.FullBalance
        else -> PaymentRequestType.FullBalance
    }

    private fun String.toPaymentProviderType(): PaymentProviderType = when (this) {
        "google_pay" -> PaymentProviderType.GooglePay
        "apple_pay" -> PaymentProviderType.ApplePay
        "stellar_usdc" -> PaymentProviderType.StellarUsdc
        "card_link" -> PaymentProviderType.CardLink
        else -> PaymentProviderType.CardLink
    }

    private fun String.toInvoicePaymentStatus(): InvoicePaymentStatus = when (this) {
        "requested" -> InvoicePaymentStatus.Requested
        "pending" -> InvoicePaymentStatus.Pending
        "paid" -> InvoicePaymentStatus.Paid
        "failed" -> InvoicePaymentStatus.Failed
        "expired" -> InvoicePaymentStatus.Expired
        else -> InvoicePaymentStatus.Pending
    }

    private companion object {
        const val DEFAULT_DEPOSIT_PERCENT = 0.30
    }
}

private val PaymentRequestType.wireName: String
    get() = when (this) {
        PaymentRequestType.Deposit -> "deposit"
        PaymentRequestType.FullBalance -> "full_balance"
    }

private val PaymentRequestType.descriptionLabel: String
    get() = when (this) {
        PaymentRequestType.Deposit -> "Deposit request"
        PaymentRequestType.FullBalance -> "Full payment request"
    }

private val PaymentProviderType.wireName: String
    get() = when (this) {
        PaymentProviderType.GooglePay -> "google_pay"
        PaymentProviderType.ApplePay -> "apple_pay"
        PaymentProviderType.StellarUsdc -> "stellar_usdc"
        PaymentProviderType.CardLink -> "card_link"
    }
