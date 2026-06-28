package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.DatabaseProvider
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import com.fordham.toolbelt.data.remote.StripeConfig
import com.fordham.toolbelt.data.remote.StripeCreatePaymentIntentRequest
import com.fordham.toolbelt.data.remote.StripePaymentBackendClient
import com.fordham.toolbelt.data.remote.StripePaymentIntentOutcome
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
import com.fordham.toolbelt.domain.model.cardterminal.CardBrand
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPaymentOutcome
import com.fordham.toolbelt.domain.model.isOnSiteCollection
import com.fordham.toolbelt.domain.model.label
import com.fordham.toolbelt.domain.model.usesStripeRail
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.PaymentRepository
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * Routes payment-link creation to the correct processor:
 * - Google Pay / Apple Pay / Card Link → Stripe Connect backend
 * - Card terminal / Tap to Pay / Bluetooth → on-site flows only (not here)
 */
class RoutedPaymentRepository(
    private val stripeBackendClient: StripePaymentBackendClient,
    private val stripeConfig: StripeConfig,
    private val authRepository: AuthRepository,
    private val databaseProvider: DatabaseProvider
) : PaymentRepository {

    private suspend fun paymentRequestDao() = databaseProvider.getDatabase().paymentRequestDao()

    override val ledger: Flow<PaymentLedgerOutcome> = flow {
        val dao = paymentRequestDao()
        emitAll(
            dao.observeAll().map { entities ->
                PaymentLedgerOutcome.Success(entities.map { it.toDomain() })
            }
        )
    }

    override suspend fun createPaymentRequest(
        invoice: Invoice,
        type: PaymentRequestType,
        provider: PaymentProviderType
    ): PaymentRequestOutcome {
        if (invoice.totalAmount.value <= 0.0) {
            return PaymentRequestOutcome.Failure(
                FailureMessage("Invoice must have a positive total before requesting payment.")
            )
        }

        return when {
            provider.isOnSiteCollection ->
                PaymentRequestOutcome.Failure(
                    FailureMessage("${provider.label} is collected on-site — use Card Terminal or Tap to Pay from the picker.")
                )
            provider.usesStripeRail -> createStripePaymentRequest(invoice, type, provider)
            else ->
                PaymentRequestOutcome.Failure(FailureMessage("Unsupported payment provider."))
        }
    }

    private suspend fun createStripePaymentRequest(
        invoice: Invoice,
        type: PaymentRequestType,
        provider: PaymentProviderType
    ): PaymentRequestOutcome {
        val amount = resolveAmount(invoice, type)
        val contractorUserId = authRepository.currentUser.firstOrNull()?.id?.value ?: "anonymous"

        if (stripeConfig.isPaymentSheetReady) {
            when (
                val intentOutcome = stripeBackendClient.createPaymentIntent(
                    StripeCreatePaymentIntentRequest(
                        amountCents = (amount * 100).toLong(),
                        invoiceId = invoice.id.value,
                        contractorUserId = contractorUserId,
                        clientName = invoice.clientName.value,
                        requestType = type.wireName,
                        paymentProvider = provider.stripeWireName,
                        applicationFeeBps = stripeConfig.applicationFeeBps
                    )
                )
            ) {
                StripePaymentIntentOutcome.NotConfigured -> Unit
                is StripePaymentIntentOutcome.Failure ->
                    return PaymentRequestOutcome.Failure(
                        error = intentOutcome.error,
                        actionUrl = intentOutcome.actionUrl
                    )
                is StripePaymentIntentOutcome.Success -> {
                    val hostedUrl = intentOutcome.response.hostedCheckoutUrl?.takeIf { it.isNotBlank() }
                    if (hostedUrl == null) {
                        return PaymentRequestOutcome.Failure(
                            FailureMessage("Stripe did not return a checkout link. Try again or use Card Terminal.")
                        )
                    }
                    val request = buildStripeRequest(
                        invoice = invoice,
                        type = type,
                        provider = provider,
                        amount = amount,
                        paymentLink = hostedUrl,
                        externalId = intentOutcome.response.checkoutSessionId
                            ?.takeIf { it.isNotBlank() }
                            ?: intentOutcome.response.paymentIntentId.takeIf { it.isNotBlank() }
                            ?: "stripe-${randomUUID()}"
                    )
                    paymentRequestDao().upsert(request.toEntity())
                    return PaymentRequestOutcome.Success(request)
                }
            }
        }

        val demoLink = demoStripeCheckoutUrl(provider, invoice.id.value, amount)
        val request = buildStripeRequest(
            invoice = invoice,
            type = type,
            provider = provider,
            amount = amount,
            paymentLink = demoLink,
            externalId = "demo-stripe-${randomUUID()}"
        )
        paymentRequestDao().upsert(request.toEntity())
        return PaymentRequestOutcome.Success(request)
    }

    override suspend fun refreshLedger(): PaymentLedgerOutcome {
        // Stripe ledger updates post via webhook events. Here we return the local database state.
        return PaymentLedgerOutcome.Success(paymentRequestDao().getAll().map { it.toDomain() })
    }

    override suspend fun markInvoicePaid(
        invoiceId: InvoiceId,
        paidAtMillis: Long
    ): PaymentLedgerOutcome {
        paymentRequestDao().markInvoicePaid(
            invoiceId = invoiceId.value,
            status = "paid",
            paidAtMillis = paidAtMillis,
            transactionHash = null,
            explorerUrl = null
        )
        return PaymentLedgerOutcome.Success(paymentRequestDao().getAll().map { it.toDomain() })
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
        paymentRequestDao().upsert(request.toEntity())
        return CardTerminalPaymentOutcome.Success(request)
    }

    private fun buildStripeRequest(
        invoice: Invoice,
        type: PaymentRequestType,
        provider: PaymentProviderType,
        amount: Double,
        paymentLink: String,
        externalId: String
    ): InvoicePaymentRequest = InvoicePaymentRequest(
        id = PaymentRequestId(externalId),
        invoiceId = invoice.id,
        invoiceClientName = invoice.clientName.value,
        type = type,
        provider = provider,
        requestedAmount = MoneyAmount(amount),
        status = InvoicePaymentStatus.Pending,
        paymentLink = PaymentLinkUrl(paymentLink)
    )

    private fun demoStripeCheckoutUrl(
        provider: PaymentProviderType,
        invoiceId: String,
        amount: Double
    ): String =
        "https://checkout.stripe.com/demo/${provider.stripeWireName}/$invoiceId?amount=${amount.toCents()}"

    private fun Double.toCents(): Long = (this * 100).toLong()

    private fun resolveAmount(invoice: Invoice, type: PaymentRequestType): Double = when (type) {
        PaymentRequestType.Deposit ->
            invoice.depositAmount.value.takeIf { it > 0.0 } ?: (invoice.totalAmount.value * DEFAULT_DEPOSIT_PERCENT)
        PaymentRequestType.FullBalance -> invoice.totalAmount.value
    }

    private companion object {
        const val DEFAULT_DEPOSIT_PERCENT = 0.30
    }
}

private val PaymentProviderType.stripeWireName: String
    get() = when (this) {
        PaymentProviderType.GooglePay -> "google_pay"
        PaymentProviderType.ApplePay -> "apple_pay"
        PaymentProviderType.CardLink -> "card_link"
        else -> "card_link"
    }

private val PaymentRequestType.wireName: String
    get() = when (this) {
        PaymentRequestType.Deposit -> "deposit"
        PaymentRequestType.FullBalance -> "full_balance"
    }
