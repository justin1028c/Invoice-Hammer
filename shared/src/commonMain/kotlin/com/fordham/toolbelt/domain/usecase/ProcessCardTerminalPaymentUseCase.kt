package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalDraft
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPaymentOutcome
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalValidationOutcome
import com.fordham.toolbelt.domain.repository.CardTerminalGatewayOutcome
import com.fordham.toolbelt.domain.repository.CardTerminalPaymentGateway
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.PaymentRepository
import com.fordham.toolbelt.domain.util.CardTerminalValidator
import kotlinx.datetime.Clock

/**
 * Manual card entry (typed PAN) is free-tier — monetize via processor margin, not a paywall.
 * Pro gates physical readers, recurring billing, and instant payouts (separate features).
 */
class ProcessCardTerminalPaymentUseCase(
    private val paymentRepository: PaymentRepository,
    private val invoiceRepository: InvoiceRepository,
    private val cardTerminalGateway: CardTerminalPaymentGateway
) {
    suspend operator fun invoke(
        invoice: Invoice,
        type: PaymentRequestType,
        draft: CardTerminalDraft
    ): CardTerminalPaymentOutcome {
        when (val validation = CardTerminalValidator.validate(draft)) {
            is CardTerminalValidationOutcome.Invalid ->
                return CardTerminalPaymentOutcome.Failure(validation.message)
            CardTerminalValidationOutcome.Valid -> Unit
        }

        if (invoice.totalAmount <= 0.0) {
            return CardTerminalPaymentOutcome.Failure(
                FailureMessage("Invoice must have a positive total before charging.")
            )
        }

        val pan = draft.panDigits.filter { it.isDigit() }
        val brand = CardTerminalValidator.detectBrand(pan)
        val lastFour = pan.takeLast(4)

        val amount = when (type) {
            PaymentRequestType.Deposit ->
                invoice.depositAmount.takeIf { it > 0.0 } ?: invoice.totalAmount * DEFAULT_DEPOSIT_PERCENT
            PaymentRequestType.FullBalance -> invoice.totalAmount
        }

        when (val gatewayOutcome = cardTerminalGateway.process(lastFour, brand, amount)) {
            is CardTerminalGatewayOutcome.Failure ->
                return CardTerminalPaymentOutcome.Failure(gatewayOutcome.error)
            CardTerminalGatewayOutcome.Success -> Unit
        }

        val paidAt = Clock.System.now().toEpochMilliseconds()
        when (
            val recorded = paymentRepository.recordCardTerminalPayment(
                invoice = invoice,
                type = type,
                amount = amount,
                lastFourDigits = lastFour,
                brand = brand,
                paidAtMillis = paidAt
            )
        ) {
            is CardTerminalPaymentOutcome.Failure -> return recorded
            is CardTerminalPaymentOutcome.Success -> {
                invoiceRepository.updateInvoice(invoice.copy(isPaid = true))
                return recorded
            }
        }
    }

    private companion object {
        const val DEFAULT_DEPOSIT_PERCENT = 0.30
    }
}
