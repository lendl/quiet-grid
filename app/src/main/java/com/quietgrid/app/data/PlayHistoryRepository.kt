package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
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
    val isChallenger: Boolean = false,
    val puzzlesSolved: Int? = null,
    val dailyDate: String? = null,
    val shareDetail: String? = null,
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
            migrateLegacyStats()
            migrated = true
        }
    }

    private suspend fun migrateLegacyStats() {
        val prefs = dataStore.data.first()
        val nonBetaGames = GameCatalog.games.filter { !it.beta }
        var timestamp = LEGACY_STATS_BASE_TIMESTAMP
        val legacyEntities = mutableListOf<PlayRecordEntity>()
        val keysToRemove = mutableListOf<Preferences.Key<*>>()

        nonBetaGames.forEach { meta ->
            val statsKey = statsKeyFor(meta.id)
            prefs[statsKey]?.let { raw ->
                runCatching { json.decodeFromString<GameStats>(raw) }.getOrNull()?.let { stats ->
                    stats.byDifficulty.forEach { (difficultyKey, diffStats) ->
                        legacyEntities += buildLegacySoloRecords(meta.id.key, difficultyKey, diffStats, timestamp)
                        timestamp += diffStats.played + 1L
                    }
                    keysToRemove += statsKey
                }
            }

            val challengerKey = statsChallengerKeyFor(meta.id)
            prefs[challengerKey]?.let { raw ->
                runCatching { json.decodeFromString<DifficultyStats>(raw) }.getOrNull()?.let { stats ->
                    legacyEntities += buildLegacyChallengerRecords(meta.id.key, stats, timestamp)
                    timestamp += stats.played + 1L
                    keysToRemove += challengerKey
                }
            }
        }

        if (legacyEntities.isNotEmpty()) {
            dao.insertAll(legacyEntities)
        }
        if (keysToRemove.isNotEmpty()) {
            dataStore.edit { editable -> keysToRemove.forEach { editable.remove(it) } }
        }
    }

    private fun buildLegacySoloRecords(
        gameId: String,
        difficulty: String,
        stats: DifficultyStats,
        startTimestamp: Long,
    ): List<PlayRecordEntity> {
        val streakCount = stats.currentStreak.coerceIn(0, stats.solved)
        val nonStreakSolvedCount = stats.solved - streakCount
        val lossCount = (stats.played - stats.solved).coerceAtLeast(0)
        val entities = mutableListOf<PlayRecordEntity>()
        var timestamp = startTimestamp

        fun addRecord(solved: Boolean) {
            entities += PlayRecordEntity(
                gameId = gameId,
                difficulty = difficulty,
                puzzleId = null,
                solved = solved,
                score = if (solved) stats.bestScore else 0,
                elapsedSeconds = 0,
                timestampMillis = timestamp,
                lossReason = if (solved) null else LEGACY_LOSS_REASON,
                isChallenger = false,
                puzzlesSolved = null,
            )
            timestamp += 1
        }

        repeat(nonStreakSolvedCount) { addRecord(solved = true) }
        repeat(lossCount) { addRecord(solved = false) }
        repeat(streakCount) { addRecord(solved = true) }
        return entities
    }

    private fun buildLegacyChallengerRecords(
        gameId: String,
        stats: DifficultyStats,
        startTimestamp: Long,
    ): List<PlayRecordEntity> {
        if (stats.played <= 0) return emptyList()
        return (0 until stats.played).map { index ->
            val isLast = index == stats.played - 1
            PlayRecordEntity(
                gameId = gameId,
                difficulty = Difficulty.EASY.key,
                puzzleId = null,
                solved = true,
                score = if (isLast) stats.bestScore else 0,
                elapsedSeconds = 0,
                timestampMillis = startTimestamp + index,
                lossReason = LEGACY_LOSS_REASON,
                isChallenger = true,
                puzzlesSolved = if (isLast) stats.solved else 0,
            )
        }
    }
}

private const val LEGACY_STATS_BASE_TIMESTAMP = 1577836800000L
private const val LEGACY_STATS_TIMESTAMP_WINDOW_MILLIS = 86_400_000L
private const val LEGACY_LOSS_REASON = "legacy"

fun isLegacyMigratedTimestamp(timestampMillis: Long): Boolean =
    timestampMillis in LEGACY_STATS_BASE_TIMESTAMP until LEGACY_STATS_BASE_TIMESTAMP + LEGACY_STATS_TIMESTAMP_WINDOW_MILLIS

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
