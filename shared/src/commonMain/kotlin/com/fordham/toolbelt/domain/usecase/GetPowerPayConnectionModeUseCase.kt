package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.PowerPayConnectionMode
import com.fordham.toolbelt.domain.repository.PowerPayIntegrationRepository

class GetPowerPayConnectionModeUseCase(
    private val integrationRepository: PowerPayIntegrationRepository
) {
    operator fun invoke(): PowerPayConnectionMode = integrationRepository.getConnectionMode()
}
