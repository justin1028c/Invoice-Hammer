package com.fordham.toolbelt.data.implementation

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
import com.fordham.toolbelt.domain.model.cardterminal.CardBrand
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPaymentOutcome
import com.fordham.toolbelt.domain.repository.PaymentRepository
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockPaymentRepository : PaymentRepository {
    private val requests = MutableStateFlow<PaymentLedgerOutcome>(PaymentLedgerOutcome.Success(emptyList()))

    override val ledger: Flow<PaymentLedgerOutcome> = requests.asStateFlow()

    override suspend fun createPaymentRequest(invoice: Invoice, type: PaymentRequestType, provider: PaymentProviderType): PaymentRequestOutcome {
        if (invoice.totalAmount.value <= 0.0) {
            return PaymentRequestOutcome.Failure(FailureMessage("Invoice must have a positive total before requesting payment."))
        }

        val amount = when (type) {
            PaymentRequestType.Deposit -> invoice.depositAmount.value.takeIf { it > 0.0 } ?: (invoice.totalAmount.value * DEFAULT_DEPOSIT_PERCENT)
            PaymentRequestType.FullBalance -> invoice.totalAmount.value
        }

        val request = InvoicePaymentRequest(
            id = PaymentRequestId(randomUUID()),
            invoiceId = invoice.id,
            invoiceClientName = invoice.clientName.value,
            type = type,
            provider = provider,
            requestedAmount = MoneyAmount(amount),
            status = InvoicePaymentStatus.Requested,
            paymentLink = PaymentLinkUrl("https://pay.invoicehammer.dev/mock/${provider.pathSegment}/${invoice.id.value}")
        )

        val current = (requests.value as? PaymentLedgerOutcome.Success)?.requests.orEmpty()
        requests.value = PaymentLedgerOutcome.Success(listOf(request) + current.filterNot { it.invoiceId == invoice.id && it.type == type && it.provider == provider })

        return PaymentRequestOutcome.Success(request)
    }

    override suspend fun refreshLedger(): PaymentLedgerOutcome {
        return requests.value
    }

    override suspend fun markInvoicePaid(
        invoiceId: InvoiceId,
        paidAtMillis: Long
    ): PaymentLedgerOutcome {
        val current = (requests.value as? PaymentLedgerOutcome.Success)?.requests.orEmpty()
        val updated = current.map { request ->
            if (request.invoiceId == invoiceId) {
                request.copy(
                    status = InvoicePaymentStatus.Paid,
                    paidAtMillis = paidAtMillis
                )
            } else {
                request
            }
        }
        val outcome = PaymentLedgerOutcome.Success(updated)
        requests.value = outcome
        return outcome
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
            invoiceClientName = invoice.clientName.value,
            type = type,
            provider = PaymentProviderType.CardTerminal,
            requestedAmount = MoneyAmount(amount),
            status = InvoicePaymentStatus.Paid,
            paymentLink = PaymentLinkUrl("terminal://${brand.name.lowercase()}/••••$lastFourDigits"),
            paidAtMillis = paidAtMillis
        )
        val current = (requests.value as? PaymentLedgerOutcome.Success)?.requests.orEmpty()
        requests.value = PaymentLedgerOutcome.Success(listOf(request) + current)
        return CardTerminalPaymentOutcome.Success(request)
    }

    private companion object {
        const val DEFAULT_DEPOSIT_PERCENT = 0.30
    }
}

private val PaymentProviderType.pathSegment: String
    get() = when (this) {
        PaymentProviderType.GooglePay -> "google-pay"
        PaymentProviderType.ApplePay -> "apple-pay"
        PaymentProviderType.CardLink -> "card"
        PaymentProviderType.CardTerminal -> "card-terminal"
        PaymentProviderType.TapToPay -> "tap-to-pay"
        PaymentProviderType.BluetoothReader -> "bluetooth-reader"
    }
