package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ClientOutcome
import com.fordham.toolbelt.domain.repository.ClientRepository

/**
 * Responsibility: Logic for deleting a client from the directory.
 */
class DeleteClientUseCase(
    private val repository: ClientRepository
) {
    suspend operator fun invoke(client: Client): ClientOutcome {
        return repository.deleteClient(client)
    }
}
