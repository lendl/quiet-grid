package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PlayRecord(
    val gameId: String,
    val difficulty: String,
    val puzzleId: String? = null,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val timestampMillis: Long,
    val lossReason: String? = null,
)

interface PlayHistoryStore {
    fun allRecords(): Flow<List<PlayRecord>>
    fun recordsFor(gameId: GameId): Flow<List<PlayRecord>>
    fun recordsForPuzzle(gameId: GameId, puzzleId: String, difficulty: Difficulty): Flow<List<PlayRecord>>
    suspend fun appendRecord(record: PlayRecord)
    suspend fun clear()
}

private val json = Json { ignoreUnknownKeys = true }
private val PLAY_HISTORY_KEY = stringPreferencesKey("play_history")

@Singleton
class PlayHistoryRepository @Inject constructor(private val dataStore: DataStore<Preferences>) : PlayHistoryStore {
    override fun allRecords(): Flow<List<PlayRecord>> = dataStore.data.map { prefs ->
        prefs[PLAY_HISTORY_KEY]?.let { raw ->
            runCatching { json.decodeFromString<List<PlayRecord>>(raw) }.getOrNull()
        } ?: emptyList()
    }

    override fun recordsFor(gameId: GameId): Flow<List<PlayRecord>> =
        allRecords().map { records -> records.filter { it.gameId == gameId.key } }

    override fun recordsForPuzzle(gameId: GameId, puzzleId: String, difficulty: Difficulty): Flow<List<PlayRecord>> =
        allRecords().map { records ->
            records.filter { it.gameId == gameId.key && it.puzzleId == puzzleId && it.difficulty == difficulty.key }
        }

    override suspend fun appendRecord(record: PlayRecord) {
        dataStore.edit { prefs ->
            val current = prefs[PLAY_HISTORY_KEY]?.let { raw ->
                runCatching { json.decodeFromString<List<PlayRecord>>(raw) }.getOrNull()
            } ?: emptyList()
            prefs[PLAY_HISTORY_KEY] = json.encodeToString(current + record)
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(PLAY_HISTORY_KEY) }
    }
}
