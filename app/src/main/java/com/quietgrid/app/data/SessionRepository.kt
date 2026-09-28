package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ActiveSessionEnvelope(
    val gameId: String,
    val elapsedSeconds: Double,
    val payload: String,
    val dailyDate: String? = null,
    val dailyTier: String? = null,
)

data class ActiveSessionSummary(val gameId: String, val dailyDate: String?)

interface SessionStore {
    val activeSession: Flow<ActiveSessionEnvelope?>
    suspend fun save(envelope: ActiveSessionEnvelope)
    suspend fun clear()
}

private val json = Json { ignoreUnknownKeys = true }
private val ACTIVE_SESSION_KEY = stringPreferencesKey("active_session")

@Singleton
class SessionRepository @Inject constructor(private val dataStore: DataStore<Preferences>) : SessionStore {
    override val activeSession: Flow<ActiveSessionEnvelope?> = dataStore.data
        .map { prefs -> prefs[ACTIVE_SESSION_KEY] }
        .distinctUntilChanged()
        .map { raw -> raw?.let { runCatching { json.decodeFromString<ActiveSessionEnvelope>(it) }.getOrNull() } }

    val activeSessionSummary: Flow<ActiveSessionSummary?> = activeSession
        .map { envelope -> envelope?.let { ActiveSessionSummary(it.gameId, it.dailyDate) } }
        .distinctUntilChanged()

    override suspend fun save(envelope: ActiveSessionEnvelope) {
        dataStore.edit { prefs ->
            prefs[ACTIVE_SESSION_KEY] = json.encodeToString(envelope)
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(ACTIVE_SESSION_KEY) }
    }
}
