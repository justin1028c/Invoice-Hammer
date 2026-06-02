package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.InvoiceOutcome
import com.fordham.toolbelt.domain.model.PowerPayEvent
import com.fordham.toolbelt.domain.model.PowerPayEventHandleOutcome
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.PaymentRepository
import kotlinx.datetime.Clock

class HandlePowerPayEventUseCase(
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(event: PowerPayEvent): PowerPayEventHandleOutcome {
        return when (event) {
            is PowerPayEvent.InvoicePaid -> handleInvoicePaid(event)
            is PowerPayEvent.DepositReceived,
            is PowerPayEvent.MilestoneReleased -> PowerPayEventHandleOutcome.Handled
        }
    }

    private suspend fun handleInvoicePaid(event: PowerPayEvent.InvoicePaid): PowerPayEventHandleOutcome {
        val invoice = invoiceRepository.getInvoiceById(event.invoiceId)
            ?: return PowerPayEventHandleOutcome.Ignored("No local invoice for ${event.invoiceId.value}")

        if (!invoice.isPaid) {
            when (val updateOutcome = invoiceRepository.updateInvoice(invoice.copy(isPaid = true))) {
                is InvoiceOutcome.Failure ->
                    return PowerPayEventHandleOutcome.Failure(updateOutcome.error)
                InvoiceOutcome.Success -> Unit
            }
        }

        paymentRepository.refreshLedger()
        paymentRepository.markInvoicePaid(
            invoiceId = event.invoiceId,
            paidAtMillis = Clock.System.now().toEpochMilliseconds(),
            transactionHash = event.transactionHash,
            explorerUrl = event.explorerUrl
        )

        return PowerPayEventHandleOutcome.Handled
    }
}
