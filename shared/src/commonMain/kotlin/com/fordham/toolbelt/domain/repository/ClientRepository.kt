package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ClientOutcome
import com.fordham.toolbelt.domain.model.ClientListOutcome
import kotlinx.coroutines.flow.Flow

interface ClientRepository {
    fun getAllClients(): Flow<ClientListOutcome>
    suspend fun searchClients(query: String): List<Client>
    suspend fun insertClient(client: Client): ClientOutcome
    suspend fun deleteClient(client: Client): ClientOutcome
}