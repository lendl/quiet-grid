package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
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
class PlayHistoryRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val dao: PlayHistoryDao,
) : PlayHistoryStore {
    private val migrationMutex = Mutex()
    @Volatile private var migrated = false

    override fun allRecords(): Flow<List<PlayRecord>> = flow {
        migrateIfNeeded()
        emitAll(dao.allRecords().map { entities -> entities.map { it.toRecord() } })
    }

    override fun recordsFor(gameId: GameId): Flow<List<PlayRecord>> = flow {
        migrateIfNeeded()
        emitAll(dao.recordsFor(gameId.key).map { entities -> entities.map { it.toRecord() } })
    }

    override fun recordsForPuzzle(gameId: GameId, puzzleId: String, difficulty: Difficulty): Flow<List<PlayRecord>> = flow {
        migrateIfNeeded()
        emitAll(dao.recordsForPuzzle(gameId.key, puzzleId, difficulty.key).map { entities -> entities.map { it.toRecord() } })
    }

    override suspend fun appendRecord(record: PlayRecord) {
        migrateIfNeeded()
        dao.insert(record.toEntity())
    }

    override suspend fun clear() {
        migrateIfNeeded()
        dao.clear()
    }

    private suspend fun migrateIfNeeded() {
        if (migrated) return
        migrationMutex.withLock {
            if (migrated) return
            val raw = dataStore.data.first()[PLAY_HISTORY_KEY]
            if (raw != null) {
                val old = runCatching { json.decodeFromString<List<PlayRecord>>(raw) }.getOrNull()
                if (!old.isNullOrEmpty()) {
                    dao.insertAll(old.map { it.toEntity() })
                }
                dataStore.edit { it.remove(PLAY_HISTORY_KEY) }
            }
            migrated = true
        }
    }
}

private const val DEFAULT_RECENT_HISTORY_WINDOW = 10

suspend fun PlayHistoryStore.recentlyPlayedPuzzleIds(
    gameId: GameId,
    difficulty: Difficulty,
    window: Int = DEFAULT_RECENT_HISTORY_WINDOW,
): Set<String> = recordsFor(gameId).first()
    .filter { it.difficulty == difficulty.key }
    .sortedByDescending { it.timestampMillis }
    .take(window)
    .mapNotNull { it.puzzleId }
    .toSet()
