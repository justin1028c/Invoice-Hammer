package com.fordham.toolbelt.data.implementation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fordham.toolbelt.domain.repository.ForemanSessionPersistencePort
import com.fordham.toolbelt.domain.repository.PersistedForemanState
import kotlinx.coroutines.flow.first
import com.fordham.toolbelt.data.dto.ForemanSessionPersistenceDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataStoreForemanSessionPersistence(
    private val dataStore: DataStore<Preferences>
) : ForemanSessionPersistencePort {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun load(): PersistedForemanState? {
        val raw = dataStore.data.first()[SESSION_JSON] ?: return null
        return runCatching {
            ForemanSessionPersistenceMapper.fromDto(json.decodeFromString(raw))
        }.getOrNull()
    }

    override suspend fun save(state: PersistedForemanState) {
        val dto = ForemanSessionPersistenceMapper.toDto(state)
        dataStore.edit { prefs ->
            prefs[SESSION_JSON] = json.encodeToString(ForemanSessionPersistenceDto.serializer(), dto)
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(SESSION_JSON)
        }
    }

    companion object {
        private val SESSION_JSON = stringPreferencesKey("foreman_session_v1")
    }
}
