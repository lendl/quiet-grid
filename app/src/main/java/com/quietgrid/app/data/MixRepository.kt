package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quietgrid.app.core.mix.Mix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class MixesEnvelope(val mixes: List<Mix> = emptyList())

private val mixJson = Json { ignoreUnknownKeys = true }
private val MIXES_KEY = stringPreferencesKey("mixes")
private val ACTIVE_MIX_ID_KEY = stringPreferencesKey("active_mix_id")

private fun Preferences.decodeMixes(): MixesEnvelope =
    this[MIXES_KEY]?.let { raw -> runCatching { mixJson.decodeFromString<MixesEnvelope>(raw) }.getOrNull() } ?: MixesEnvelope()

@Singleton
class MixRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {
    val mixes: Flow<List<Mix>> = dataStore.data.map { it.decodeMixes().mixes }

    val activeMixId: Flow<String?> = dataStore.data.map { it[ACTIVE_MIX_ID_KEY] }

    suspend fun saveMix(mix: Mix) {
        dataStore.edit { prefs ->
            val current = prefs.decodeMixes().mixes
            val updated = if (current.any { it.id == mix.id }) {
                current.map { if (it.id == mix.id) mix else it }
            } else {
                current + mix
            }
            prefs[MIXES_KEY] = mixJson.encodeToString(MixesEnvelope(updated))
        }
    }

    suspend fun deleteMix(mixId: String) {
        dataStore.edit { prefs ->
            val updated = prefs.decodeMixes().mixes.filterNot { it.id == mixId }
            prefs[MIXES_KEY] = mixJson.encodeToString(MixesEnvelope(updated))
            if (prefs[ACTIVE_MIX_ID_KEY] == mixId) prefs.remove(ACTIVE_MIX_ID_KEY)
        }
    }

    suspend fun setActiveMix(mixId: String) {
        dataStore.edit { it[ACTIVE_MIX_ID_KEY] = mixId }
    }

    suspend fun clearActiveMix() {
        dataStore.edit { it.remove(ACTIVE_MIX_ID_KEY) }
    }
}
