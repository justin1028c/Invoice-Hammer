package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.DatabaseProvider
import com.fordham.toolbelt.data.toDomain
import com.fordham.toolbelt.data.toEntity
import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ClientOutcome
import com.fordham.toolbelt.domain.model.ClientListOutcome
import com.fordham.toolbelt.domain.repository.ClientRepository
import com.fordham.toolbelt.data.SyncQueueEntity
import com.fordham.toolbelt.util.PlatformActions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.datetime.Clock

public class RoomClientRepository(
    private val databaseProvider: DatabaseProvider,
    private val platformActions: PlatformActions
) : ClientRepository {

    private suspend fun clientDao() = databaseProvider.getDatabase().clientDao()
    private suspend fun syncQueueDao() = databaseProvider.getDatabase().syncQueueDao()

    override fun getAllClients(): Flow<ClientListOutcome> = flow {
        val dao = clientDao()
        emitAll(
            dao.getAllClients()
                .map { list -> ClientListOutcome.Success(list.map { it.toDomain() }) as ClientListOutcome }
        )
    }.catch { emit(ClientListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to list clients"))) }
    
    override suspend fun searchClients(query: String): List<Client> =
        clientDao().searchClients(query).map { it.toDomain() }

    override suspend fun insertClient(client: Client): ClientOutcome = try {
        clientDao().insertClient(client.toEntity())
        syncQueueDao().enqueue(
            SyncQueueEntity(
                operationType = "BACKUP",
                createdAtMillis = Clock.System.now().toEpochMilliseconds()
            )
        )
        platformActions.triggerBackgroundSync()
        ClientOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomClientRepository", "repository", e)
        ClientOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert client"))
    }

    override suspend fun replaceAllClients(clients: List<Client>): ClientOutcome = try {
        clientDao().deleteAllClients()
        if (clients.isNotEmpty()) {
            clientDao().insertClients(clients.map { it.toEntity() })
        }
        ClientOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomClientRepository", "repository", e)
        ClientOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to restore clients"))
    }

    override suspend fun deleteClient(client: Client): ClientOutcome = try {
        clientDao().deleteClient(client.toEntity())
        syncQueueDao().enqueue(
            SyncQueueEntity(
                operationType = "BACKUP",
                createdAtMillis = Clock.System.now().toEpochMilliseconds()
            )
        )
        platformActions.triggerBackgroundSync()
        ClientOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomClientRepository", "repository", e)
        ClientOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to delete client"))
    }
}

private const val TAG = "RoomClientRepository"
