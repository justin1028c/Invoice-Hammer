package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ClientOutcome
import com.fordham.toolbelt.domain.repository.ClientRepository

/**
 * Responsibility: Updates or creates a client profile in the directory.
 * Follows the Clean Architecture Firewall pattern.
 */
class UpdateClientUseCase(
    private val repository: ClientRepository
) {
    suspend operator fun invoke(client: Client): ClientOutcome {
        return repository.insertClient(client)
    }
}
