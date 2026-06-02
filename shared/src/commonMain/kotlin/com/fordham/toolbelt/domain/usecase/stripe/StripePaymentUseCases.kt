package com.fordham.toolbelt.domain.usecase.stripe

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.model.stripe.BluetoothReaderOutcome
import com.fordham.toolbelt.domain.model.stripe.CreateStripePaymentIntentCommand
import com.fordham.toolbelt.domain.model.stripe.CreateStripePaymentIntentOutcome
import com.fordham.toolbelt.domain.model.stripe.StripeCardCollectOutcome
import com.fordham.toolbelt.domain.model.stripe.StripePaymentChannel
import com.fordham.toolbelt.domain.model.stripe.StripePaymentMode
import com.fordham.toolbelt.domain.model.stripe.TapToPayOutcome
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.BluetoothCardReaderGateway
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.PaymentRepository
import com.fordham.toolbelt.domain.repository.StripeIntegrationRepository
import com.fordham.toolbelt.domain.repository.StripePaymentIntentRepository
import com.fordham.toolbelt.domain.repository.StripePaymentSheetGateway
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.repository.TapToPayGateway
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

class GetStripePaymentModeUseCase(
    private val stripeIntegration: StripeIntegrationRepository
) {
    operator fun invoke(): StripePaymentMode =
        if (stripeIntegration.isPaymentSheetReady) {
            StripePaymentMode.PaymentSheet
        } else {
            StripePaymentMode.ManualEntrySimulator
        }
}

class ProcessStripePaymentSheetUseCase(
    private val stripeIntegration: StripeIntegrationRepository,
    private val paymentIntentRepository: StripePaymentIntentRepository,
    private val authRepository: AuthRepository,
    private val paymentSheetGateway: StripePaymentSheetGateway,
    private val paymentRepository: PaymentRepository,
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(invoice: Invoice, type: PaymentRequestType): StripeCardCollectOutcome {
        if (!stripeIntegration.isPaymentSheetReady) {
            return StripeCardCollectOutcome.Failure(
                FailureMessage("Add stripe.publishable.key and stripe.payment.backend.url to enable secure checkout.")
            )
        }

        val amount = resolveAmount(invoice, type)
        val contractorId = authRepository.currentUser.first()?.id?.value ?: "anonymous"
        val intentOutcome = paymentIntentRepository.createPaymentIntent(
            CreateStripePaymentIntentCommand(
                amountCents = (amount * 100).toLong(),
                invoiceId = invoice.id,
                contractorUserId = contractorId,
                clientName = invoice.clientName,
                requestType = type,
                channel = StripePaymentChannel.CardTerminal
            )
        )

        val ready = when (intentOutcome) {
            CreateStripePaymentIntentOutcome.BackendNotConfigured ->
                return StripeCardCollectOutcome.Failure(FailureMessage("Payment backend is not configured."))
            is CreateStripePaymentIntentOutcome.Failure ->
                return StripeCardCollectOutcome.Failure(intentOutcome.error)
            is CreateStripePaymentIntentOutcome.Ready -> {
                if (intentOutcome.clientSecret.isBlank()) {
                    return StripeCardCollectOutcome.Failure(
                        FailureMessage("Stripe did not return a payment secret for on-site checkout.")
                    )
                }
                intentOutcome
            }
        }

        return when (
            val collected = paymentSheetGateway.presentPaymentSheet(
                invoice = invoice,
                type = type,
                clientSecret = ready.clientSecret,
                paymentIntentId = ready.paymentIntentId,
                stripeAccountId = ready.stripeAccountId
            )
        ) {
            StripeCardCollectOutcome.Cancelled -> collected
            is StripeCardCollectOutcome.Failure -> collected
            is StripeCardCollectOutcome.Success -> {
                finalizePaid(invoice, type, amount, collected.paymentIntentId.value)
                collected
            }
        }
    }

    private suspend fun finalizePaid(
        invoice: Invoice,
        type: PaymentRequestType,
        amount: Double,
        paymentIntentId: String
    ) {
        val paidAt = Clock.System.now().toEpochMilliseconds()
        paymentRepository.recordCardTerminalPayment(
            invoice = invoice,
            type = type,
            amount = amount,
            lastFourDigits = paymentIntentId.takeLast(4).filter { it.isDigit() }.padStart(4, '0').take(4),
            brand = com.fordham.toolbelt.domain.model.cardterminal.CardBrand.Unknown,
            paidAtMillis = paidAt
        )
        invoiceRepository.updateInvoice(invoice.copy(isPaid = true))
    }

    private fun resolveAmount(invoice: Invoice, type: PaymentRequestType): Double = when (type) {
        PaymentRequestType.Deposit ->
            invoice.depositAmount.takeIf { it > 0.0 } ?: invoice.totalAmount * DEFAULT_DEPOSIT_PERCENT
        PaymentRequestType.FullBalance -> invoice.totalAmount
    }

    private companion object {
        const val DEFAULT_DEPOSIT_PERCENT = 0.30
    }
}

class ProcessTapToPayUseCase(
    private val stripeIntegration: StripeIntegrationRepository,
    private val paymentIntentRepository: StripePaymentIntentRepository,
    private val authRepository: AuthRepository,
    private val tapToPayGateway: TapToPayGateway,
    private val paymentRepository: PaymentRepository,
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(invoice: Invoice, type: PaymentRequestType): TapToPayOutcome {
        if (!stripeIntegration.isPaymentSheetReady) {
            return TapToPayOutcome.Failure(
                FailureMessage("Configure Stripe keys and payment backend to enable Tap to Pay.")
            )
        }

        val amount = when (type) {
            PaymentRequestType.Deposit ->
                invoice.depositAmount.takeIf { it > 0.0 } ?: invoice.totalAmount * 0.30
            PaymentRequestType.FullBalance -> invoice.totalAmount
        }
        val contractorId = authRepository.currentUser.first()?.id?.value ?: "anonymous"

        val intentOutcome = paymentIntentRepository.createPaymentIntent(
            CreateStripePaymentIntentCommand(
                amountCents = (amount * 100).toLong(),
                invoiceId = invoice.id,
                contractorUserId = contractorId,
                clientName = invoice.clientName,
                requestType = type,
                channel = StripePaymentChannel.TapToPay
            )
        )

        val ready = when (intentOutcome) {
            CreateStripePaymentIntentOutcome.BackendNotConfigured ->
                return TapToPayOutcome.Failure(FailureMessage("Payment backend is not configured."))
            is CreateStripePaymentIntentOutcome.Failure ->
                return TapToPayOutcome.Failure(intentOutcome.error)
            is CreateStripePaymentIntentOutcome.Ready -> {
                if (intentOutcome.clientSecret.isBlank()) {
                    return TapToPayOutcome.Failure(
                        FailureMessage("Stripe did not return a payment secret for Tap to Pay.")
                    )
                }
                intentOutcome
            }
        }

        return when (
            val outcome = tapToPayGateway.collect(
                invoice = invoice,
                type = type,
                clientSecret = ready.clientSecret,
                paymentIntentId = ready.paymentIntentId,
                stripeAccountId = ready.stripeAccountId
            )
        ) {
            TapToPayOutcome.Cancelled, is TapToPayOutcome.Failure -> outcome
            is TapToPayOutcome.Success -> {
                val paidAt = Clock.System.now().toEpochMilliseconds()
                paymentRepository.recordCardTerminalPayment(
                    invoice = invoice,
                    type = type,
                    amount = amount,
                    lastFourDigits = "TAPY",
                    brand = com.fordham.toolbelt.domain.model.cardterminal.CardBrand.Unknown,
                    paidAtMillis = paidAt
                )
                invoiceRepository.updateInvoice(invoice.copy(isPaid = true))
                outcome
            }
        }
    }
}

class ProcessBluetoothReaderPaymentUseCase(
    private val hasSubscriptionFeature: HasSubscriptionFeatureUseCase,
    private val settingsRepository: SettingsRepository,
    private val stripeIntegration: StripeIntegrationRepository,
    private val paymentIntentRepository: StripePaymentIntentRepository,
    private val authRepository: AuthRepository,
    private val bluetoothGateway: BluetoothCardReaderGateway,
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(invoice: Invoice, type: PaymentRequestType): BluetoothReaderOutcome {
        val settings = settingsRepository.getBusinessSettings()
        if (!hasSubscriptionFeature(SubscriptionFeature.BluetoothCardReader) && !settings.isPremium) {
            return BluetoothReaderOutcome.PremiumRequired
        }
        if (!stripeIntegration.isPaymentSheetReady) {
            return BluetoothReaderOutcome.Failure(
                FailureMessage("Stripe Connect backend required for physical readers.")
            )
        }

        val amount = when (type) {
            PaymentRequestType.Deposit ->
                invoice.depositAmount.takeIf { it > 0.0 } ?: invoice.totalAmount * 0.30
            PaymentRequestType.FullBalance -> invoice.totalAmount
        }
        val contractorId = authRepository.currentUser.first()?.id?.value ?: "anonymous"
        val intentOutcome = paymentIntentRepository.createPaymentIntent(
            CreateStripePaymentIntentCommand(
                amountCents = (amount * 100).toLong(),
                invoiceId = invoice.id,
                contractorUserId = contractorId,
                clientName = invoice.clientName,
                requestType = type,
                channel = StripePaymentChannel.BluetoothReader
            )
        )

        val ready = when (intentOutcome) {
            CreateStripePaymentIntentOutcome.BackendNotConfigured ->
                return BluetoothReaderOutcome.Failure(FailureMessage("Payment backend is not configured."))
            is CreateStripePaymentIntentOutcome.Failure ->
                return BluetoothReaderOutcome.Failure(intentOutcome.error)
            is CreateStripePaymentIntentOutcome.Ready -> {
                if (intentOutcome.clientSecret.isBlank()) {
                    return BluetoothReaderOutcome.Failure(
                        FailureMessage("Stripe did not return a payment secret for the card reader.")
                    )
                }
                intentOutcome
            }
        }

        return when (
            val outcome = bluetoothGateway.collect(
                invoice = invoice,
                type = type,
                clientSecret = ready.clientSecret,
                paymentIntentId = ready.paymentIntentId,
                stripeAccountId = ready.stripeAccountId
            )
        ) {
            BluetoothReaderOutcome.Cancelled,
            BluetoothReaderOutcome.PremiumRequired,
            BluetoothReaderOutcome.ReaderNotAvailable,
            is BluetoothReaderOutcome.Failure -> outcome
            is BluetoothReaderOutcome.Success -> {
                invoiceRepository.updateInvoice(invoice.copy(isPaid = true))
                outcome
            }
        }
    }
}
