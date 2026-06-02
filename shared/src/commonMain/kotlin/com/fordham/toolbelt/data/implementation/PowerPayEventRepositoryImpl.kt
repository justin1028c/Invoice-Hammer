package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayClientOutcome
import com.fordham.toolbelt.domain.model.InvoicePaymentStatus
import com.fordham.toolbelt.domain.model.PaymentLedgerOutcome
import com.fordham.toolbelt.domain.model.PowerPayEvent
import com.fordham.toolbelt.domain.repository.PaymentRepository
import com.fordham.toolbelt.domain.repository.PowerPayEventRepository
import kotlinx.datetime.Clock

class PowerPayEventRepositoryImpl(
    private val powerPayClient: PowerPayClient,
    private val paymentRepository: PaymentRepository
) : PowerPayEventRepository {
    private val handledEventIds = mutableSetOf<String>()
    private var lastPollEpochSeconds: Long = Clock.System.now().epochSeconds

    override suspend fun pollClientEvents(): List<PowerPayEvent> {
        val discovered = mutableListOf<PowerPayEvent>()

        when (val poll = powerPayClient.pollWebhookEvents(lastPollEpochSeconds)) {
            is PowerPayClientOutcome.Success -> {
                poll.value.forEach { dto ->
                    val event = PowerPayEventMapper.fromWebhookDto(dto) ?: return@forEach
                    if (handledEventIds.add(event.eventId.value)) {
                        discovered.add(event)
                    }
                }
            }
            is PowerPayClientOutcome.Failure -> {
                // Fall back to per-payment status polling when /v1/events is unavailable.
            }
        }

        val pending = when (val ledger = paymentRepository.refreshLedger()) {
            is PaymentLedgerOutcome.Success ->
                ledger.requests.filter {
                    it.status == InvoicePaymentStatus.Pending || it.status == InvoicePaymentStatus.Requested
                }
            is PaymentLedgerOutcome.Failure -> emptyList()
        }

        pending.forEach { request ->
            when (val statusOutcome = powerPayClient.getPaymentStatus(request.id.value)) {
                is PowerPayClientOutcome.Success -> {
                    val paidEvent = PowerPayEventMapper.fromPaidPayment(statusOutcome.value) ?: return@forEach
                    if (handledEventIds.add(paidEvent.eventId.value)) {
                        discovered.add(paidEvent)
                    }
                }
                is PowerPayClientOutcome.Failure -> Unit
            }
        }

        lastPollEpochSeconds = Clock.System.now().epochSeconds
        return discovered
    }
}
