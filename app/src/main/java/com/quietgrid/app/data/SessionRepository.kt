package com.quietgrid.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ActiveSessionEnvelope(
    val gameId: String,
    val elapsedSeconds: Double,
    val payload: String,
)

private val json = Json { ignoreUnknownKeys = true }
private val ACTIVE_SESSION_KEY = stringPreferencesKey("active_session")

class SessionRepository(private val context: Context) {
    val activeSession: Flow<ActiveSessionEnvelope?> = context.appDataStore.data.map { prefs ->
        prefs[ACTIVE_SESSION_KEY]?.let { raw ->
            runCatching { json.decodeFromString<ActiveSessionEnvelope>(raw) }.getOrNull()
        }
    }

    suspend fun save(envelope: ActiveSessionEnvelope) {
        context.appDataStore.edit { prefs ->
            prefs[ACTIVE_SESSION_KEY] = json.encodeToString(envelope)
        }
    }

    suspend fun clear() {
        context.appDataStore.edit { prefs -> prefs.remove(ACTIVE_SESSION_KEY) }
    }
}
