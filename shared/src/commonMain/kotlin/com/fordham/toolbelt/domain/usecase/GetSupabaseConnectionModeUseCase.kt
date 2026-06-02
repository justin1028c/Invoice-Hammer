package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.SupabaseConnectionMode
import com.fordham.toolbelt.domain.repository.SupabaseIntegrationRepository

class GetSupabaseConnectionModeUseCase(
    private val integrationRepository: SupabaseIntegrationRepository
) {
    operator fun invoke(): SupabaseConnectionMode = integrationRepository.getConnectionMode()
}
