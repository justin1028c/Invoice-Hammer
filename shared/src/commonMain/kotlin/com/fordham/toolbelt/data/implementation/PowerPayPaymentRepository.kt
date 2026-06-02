package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayClientOutcome
import com.fordham.toolbelt.data.remote.PowerPayConfig
import com.fordham.toolbelt.data.remote.PowerPayCreatePaymentRequestDto
import com.fordham.toolbelt.data.remote.PowerPayPaymentResponseDto
import com.fordham.toolbelt.data.PaymentRequestDao
import com.fordham.toolbelt.data.toDomain
import com.fordham.toolbelt.data.toEntity
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
import com.fordham.toolbelt.domain.model.StellarExplorerUrl
import com.fordham.toolbelt.domain.model.StellarTransactionHash
import com.fordham.toolbelt.domain.model.cardterminal.CardBrand
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPaymentOutcome
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.PaymentRepository
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class PowerPayPaymentRepository(
    private val powerPayClient: PowerPayClient,
    private val config: PowerPayConfig,
    private val authRepository: AuthRepository,
    private val paymentRequestDao: PaymentRequestDao
) : PaymentRepository {
    override val ledger: Flow<PaymentLedgerOutcome> =
        paymentRequestDao.observeAll().map { entities ->
            PaymentLedgerOutcome.Success(entities.map { it.toDomain() })
        }

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

        val contractorUserId = authRepository.currentUser.firstOrNull()?.id?.value ?: "anonymous"

        val outcome = powerPayClient.createInvoicePayment(
            PowerPayCreatePaymentRequestDto(
                appId = config.appId,
                contractorUserId = contractorUserId,
                invoiceId = invoice.id.value,
                clientName = invoice.clientName,
                amountUsd = amount,
                requestType = type.wireName,
                provider = provider.wireName,
                description = "${type.descriptionLabel} for ${invoice.clientName}",
                preset = config.preset,
                environment = config.environment.wireName
            )
        )

        return when (outcome) {
            is PowerPayClientOutcome.Success -> {
                val request = outcome.value.toDomain()
                paymentRequestDao.upsert(request.toEntity())
                PaymentRequestOutcome.Success(request)
            }
            is PowerPayClientOutcome.Failure -> PaymentRequestOutcome.Failure(outcome.error)
        }
    }

    override suspend fun refreshLedger(): PaymentLedgerOutcome {
        val outcome = powerPayClient.getTransactionHistory()
        return when (outcome) {
            is PowerPayClientOutcome.Success -> {
                val requests = outcome.value.map { it.toDomain() }
                paymentRequestDao.upsertAll(requests.map { it.toEntity() })
                PaymentLedgerOutcome.Success(paymentRequestDao.getAll().map { it.toDomain() })
            }
            is PowerPayClientOutcome.Failure -> PaymentLedgerOutcome.Failure(outcome.error)
        }
    }

    override suspend fun markInvoicePaid(
        invoiceId: InvoiceId,
        paidAtMillis: Long,
        transactionHash: StellarTransactionHash?,
        explorerUrl: StellarExplorerUrl?
    ): PaymentLedgerOutcome {
        paymentRequestDao.markInvoicePaid(
            invoiceId = invoiceId.value,
            status = "paid",
            paidAtMillis = paidAtMillis,
            transactionHash = transactionHash?.value,
            explorerUrl = explorerUrl?.value
        )
        return PaymentLedgerOutcome.Success(paymentRequestDao.getAll().map { it.toDomain() })
    }

    override suspend fun recordCardTerminalPayment(
        invoice: Invoice,
        type: PaymentRequestType,
        amount: Double,
        lastFourDigits: String,
        brand: CardBrand,
        paidAtMillis: Long
    ): CardTerminalPaymentOutcome {
        val request = InvoicePaymentRequest(
            id = PaymentRequestId(randomUUID()),
            invoiceId = invoice.id,
            invoiceClientName = invoice.clientName,
            type = type,
            provider = PaymentProviderType.CardTerminal,
            requestedAmount = MoneyAmount(amount),
            status = InvoicePaymentStatus.Paid,
            paymentLink = PaymentLinkUrl("terminal://${brand.name.lowercase()}/••••$lastFourDigits"),
            paidAtMillis = paidAtMillis,
            assetCode = "USD"
        )
        paymentRequestDao.upsert(request.toEntity())
        return CardTerminalPaymentOutcome.Success(request)
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
            stellarExplorerUrl = explorerUrl?.let { StellarExplorerUrl(it) },
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
        "card_terminal" -> PaymentProviderType.CardTerminal
        else -> PaymentProviderType.CardLink
    }

    private fun String.toInvoicePaymentStatus(): InvoicePaymentStatus = when (this.lowercase()) {
        "requested" -> InvoicePaymentStatus.Requested
        "pending", "unpaid" -> InvoicePaymentStatus.Pending
        "paid", "paid_in_full", "deposit_paid", "milestone_paid" -> InvoicePaymentStatus.Paid
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
        PaymentRequestType.Deposit -> "Project deposit"
        PaymentRequestType.FullBalance -> "Invoice payment"
    }

private val PaymentProviderType.wireName: String
    get() = when (this) {
        PaymentProviderType.GooglePay -> "google_pay"
        PaymentProviderType.ApplePay -> "apple_pay"
        PaymentProviderType.StellarUsdc -> "stellar_usdc"
        PaymentProviderType.CardLink -> "card_link"
        PaymentProviderType.CardTerminal -> "card_terminal"
        PaymentProviderType.TapToPay -> "tap_to_pay"
        PaymentProviderType.BluetoothReader -> "bluetooth_reader"
    }
