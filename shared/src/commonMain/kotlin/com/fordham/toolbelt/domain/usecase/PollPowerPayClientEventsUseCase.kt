package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.PowerPayEvent
import com.fordham.toolbelt.domain.repository.PowerPayEventRepository

/**
 * Polls PowerPay for client-side events (equivalent to pay.on(...) in the JS SDK)
 * and applies verified business updates (e.g. mark invoice paid).
 */
class PollPowerPayClientEventsUseCase(
    private val eventRepository: PowerPayEventRepository,
    private val handlePowerPayEventUseCase: HandlePowerPayEventUseCase
) {
    suspend operator fun invoke(): List<PowerPayEvent> {
        val events = eventRepository.pollClientEvents()
        events.forEach { handlePowerPayEventUseCase(it) }
        return events
    }
}
