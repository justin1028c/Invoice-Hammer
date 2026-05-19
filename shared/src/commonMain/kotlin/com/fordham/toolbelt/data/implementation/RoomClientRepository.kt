package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.ClientDao
import com.fordham.toolbelt.data.toDomain
import com.fordham.toolbelt.data.toEntity
import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ClientOutcome
import com.fordham.toolbelt.domain.model.ClientListOutcome
import com.fordham.toolbelt.domain.repository.ClientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch

class RoomClientRepository(
    private val clientDao: ClientDao
) : ClientRepository {
    override fun getAllClients(): Flow<ClientListOutcome> =
        clientDao.getAllClients()
            .map { list -> ClientListOutcome.Success(list.map { it.toDomain() }) as ClientListOutcome }
            .catch { emit(ClientListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to list clients"))) }
    
    override suspend fun searchClients(query: String): List<Client> =
        clientDao.searchClients(query).map { it.toDomain() }

    override suspend fun insertClient(client: Client): ClientOutcome = try {
        clientDao.insertClient(client.toEntity())
        ClientOutcome.Success
    } catch (e: Exception) {
        ClientOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert client"))
    }

    override suspend fun deleteClient(client: Client): ClientOutcome = try {
        clientDao.deleteClient(client.toEntity())
        ClientOutcome.Success
    } catch (e: Exception) {
        ClientOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to delete client"))
    }
}
