package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.PaymentRequestDao
import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayClientOutcome
import com.fordham.toolbelt.data.remote.PowerPayConfig
import com.fordham.toolbelt.data.remote.PowerPayCreatePaymentRequestDto
import com.fordham.toolbelt.data.remote.PowerPayPaymentResponseDto
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
import com.fordham.toolbelt.domain.model.StellarExplorerUrl
import com.fordham.toolbelt.domain.model.StellarTransactionHash
import com.fordham.toolbelt.domain.model.cardterminal.CardBrand
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPaymentOutcome
import com.fordham.toolbelt.domain.model.isOnSiteCollection
import com.fordham.toolbelt.domain.model.label
import com.fordham.toolbelt.domain.model.usesStripeRail
import com.fordham.toolbelt.domain.model.usesStellarRail
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.PaymentRepository
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * Routes payment-link creation to the correct processor:
 * - [PaymentProviderType.StellarUsdc] → PowerPay (Stellar USDC)
 * - Google Pay / Apple Pay / Card Link → Stripe Connect backend
 * - Card terminal / Tap to Pay / Bluetooth → on-site flows only (not here)
 */
class RoutedPaymentRepository(
    private val powerPayClient: PowerPayClient,
    private val powerPayConfig: PowerPayConfig,
    private val stripeBackendClient: StripePaymentBackendClient,
    private val stripeConfig: StripeConfig,
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
            provider.usesStellarRail -> createStellarPaymentRequest(invoice, type)
            provider.usesStripeRail -> createStripePaymentRequest(invoice, type, provider)
            else ->
                PaymentRequestOutcome.Failure(FailureMessage("Unsupported payment provider."))
        }
    }

    private suspend fun createStellarPaymentRequest(
        invoice: Invoice,
        type: PaymentRequestType
    ): PaymentRequestOutcome {
        val amount = resolveAmount(invoice, type)
        val contractorUserId = authRepository.currentUser.firstOrNull()?.id?.value ?: "anonymous"

        val outcome = powerPayClient.createInvoicePayment(
            PowerPayCreatePaymentRequestDto(
                appId = powerPayConfig.appId,
                contractorUserId = contractorUserId,
                invoiceId = invoice.id.value,
                clientName = invoice.clientName.value,
                amountUsd = amount,
                requestType = type.wireName,
                provider = STELLAR_PROVIDER_WIRE,
                description = "${type.descriptionLabel} for ${invoice.clientName.value}",
                preset = powerPayConfig.preset,
                environment = powerPayConfig.environment.wireName
            )
        )

        return when (outcome) {
            is PowerPayClientOutcome.Success -> {
                val request = outcome.value.toStellarDomain()
                paymentRequestDao.upsert(request.toEntity())
                PaymentRequestOutcome.Success(request)
            }
            is PowerPayClientOutcome.Failure -> PaymentRequestOutcome.Failure(outcome.error)
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
                    paymentRequestDao.upsert(request.toEntity())
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
        paymentRequestDao.upsert(request.toEntity())
        return PaymentRequestOutcome.Success(request)
    }

    override suspend fun refreshLedger(): PaymentLedgerOutcome {
        val outcome = powerPayClient.getTransactionHistory()
        return when (outcome) {
            is PowerPayClientOutcome.Success -> {
                val stellarRequests = outcome.value.map { it.toStellarDomain() }
                val local = paymentRequestDao.getAll().map { it.toDomain() }
                val stripeAndTerminal = local.filter { !it.provider.usesStellarRail }
                val merged = (stellarRequests + stripeAndTerminal)
                    .distinctBy { it.id.value }
                paymentRequestDao.upsertAll(merged.map { it.toEntity() })
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
            invoiceClientName = invoice.clientName.value,
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
        paymentLink = PaymentLinkUrl(paymentLink),
        assetCode = "USD"
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

    private fun PowerPayPaymentResponseDto.toStellarDomain(): InvoicePaymentRequest =
        InvoicePaymentRequest(
            id = PaymentRequestId(paymentId),
            invoiceId = InvoiceId(invoiceId),
            invoiceClientName = clientName,
            type = requestType.toPaymentRequestType(),
            provider = PaymentProviderType.StellarUsdc,
            requestedAmount = MoneyAmount(amountUsd),
            status = status.toInvoicePaymentStatus(),
            paymentLink = PaymentLinkUrl(paymentLinkUrl),
            createdAtMillis = createdAtMillis,
            stellarTransactionHash = transactionHash?.let { StellarTransactionHash(it) },
            stellarExplorerUrl = explorerUrl?.let { StellarExplorerUrl(it) },
            assetCode = assetCode.ifBlank { "USDC" }
        )

    private companion object {
        const val DEFAULT_DEPOSIT_PERCENT = 0.30
        const val STELLAR_PROVIDER_WIRE = "stellar_usdc"
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

private val PaymentRequestType.descriptionLabel: String
    get() = when (this) {
        PaymentRequestType.Deposit -> "Project deposit"
        PaymentRequestType.FullBalance -> "Invoice payment"
    }

private fun String.toPaymentRequestType(): PaymentRequestType = when (this) {
    "deposit" -> PaymentRequestType.Deposit
    else -> PaymentRequestType.FullBalance
}

private fun String.toInvoicePaymentStatus(): InvoicePaymentStatus = when (this.lowercase()) {
    "requested" -> InvoicePaymentStatus.Requested
    "pending", "unpaid" -> InvoicePaymentStatus.Pending
    "paid", "paid_in_full", "deposit_paid", "milestone_paid" -> InvoicePaymentStatus.Paid
    "failed" -> InvoicePaymentStatus.Failed
    "expired" -> InvoicePaymentStatus.Expired
    else -> InvoicePaymentStatus.Pending
}
