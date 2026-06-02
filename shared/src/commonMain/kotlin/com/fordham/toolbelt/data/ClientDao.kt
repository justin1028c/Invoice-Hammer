package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    suspend fun getAllClientsOnce(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%'")
    suspend fun searchClients(query: String): List<ClientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<ClientEntity>)

    @Query("DELETE FROM clients")
    suspend fun deleteAllClients()

    @Delete
    suspend fun deleteClient(client: ClientEntity)
}
