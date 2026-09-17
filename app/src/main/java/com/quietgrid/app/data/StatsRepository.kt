package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

internal fun statsKeyFor(gameId: GameId) = stringPreferencesKey("stats_${gameId.key}")
internal fun statsChallengerKeyFor(gameId: GameId) = stringPreferencesKey("stats_challenger_${gameId.key}")

@Serializable
data class DifficultyStats(
    val played: Int = 0,
    val solved: Int = 0,
    val bestScore: Int = 0,
    val currentStreak: Int = 0,
)

@Serializable
data class GameStats(
    val byDifficulty: Map<String, DifficultyStats> = emptyMap(),
) {
    fun forDifficulty(difficulty: Difficulty): DifficultyStats =
        byDifficulty[difficulty.key] ?: DifficultyStats()
}

interface StatsStore {
    fun statsFor(gameId: GameId): Flow<GameStats>
    suspend fun recordResult(gameId: GameId, difficulty: Difficulty, solved: Boolean, score: Int)
    fun challengerStatsFor(gameId: GameId): Flow<DifficultyStats>
    suspend fun recordChallengerResult(gameId: GameId, puzzlesSolved: Int, score: Int)
}

private val json = Json { ignoreUnknownKeys = true }

@Singleton
class StatsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val historyStore: PlayHistoryStore,
) : StatsStore {
    fun statsForGames(gameIds: List<GameId>): Flow<Map<GameId, GameStats>> =
        combine(gameIds.map { id -> statsFor(id).map { id to it } }) { pairs -> pairs.toMap() }

    override fun statsFor(gameId: GameId): Flow<GameStats> =
        if (GameCatalog.get(gameId).beta) {
            dataStore.data.map { prefs ->
                prefs[statsKeyFor(gameId)]?.let { raw ->
                    runCatching { json.decodeFromString<GameStats>(raw) }.getOrNull()
                } ?: GameStats()
            }
        } else {
            historyStore.recordsFor(gameId).map { deriveGameStats(it) }
        }

    override suspend fun recordResult(gameId: GameId, difficulty: Difficulty, solved: Boolean, score: Int) {
        if (!GameCatalog.get(gameId).beta) return
        dataStore.edit { prefs ->
            val key = statsKeyFor(gameId)
            val current = prefs[key]?.let { runCatching { json.decodeFromString<GameStats>(it) }.getOrNull() }
                ?: GameStats()
            val existing = current.forDifficulty(difficulty)
            val updated = existing.copy(
                played = existing.played + 1,
                solved = existing.solved + if (solved) 1 else 0,
                bestScore = if (solved) maxOf(existing.bestScore, score) else existing.bestScore,
                currentStreak = if (solved) existing.currentStreak + 1 else 0,
            )
            val newMap = current.byDifficulty.toMutableMap().apply { put(difficulty.key, updated) }
            prefs[key] = json.encodeToString(GameStats(byDifficulty = newMap))
        }
    }

    override fun challengerStatsFor(gameId: GameId): Flow<DifficultyStats> =
        if (GameCatalog.get(gameId).beta) {
            dataStore.data.map { prefs ->
                prefs[statsChallengerKeyFor(gameId)]?.let { raw ->
                    runCatching { json.decodeFromString<DifficultyStats>(raw) }.getOrNull()
                } ?: DifficultyStats()
            }
        } else {
            historyStore.recordsFor(gameId).map { deriveChallengerStats(it) }
        }

    override suspend fun recordChallengerResult(gameId: GameId, puzzlesSolved: Int, score: Int) {
        if (!GameCatalog.get(gameId).beta) return
        dataStore.edit { prefs ->
            val key = statsChallengerKeyFor(gameId)
            val existing = prefs[key]?.let { runCatching { json.decodeFromString<DifficultyStats>(it) }.getOrNull() }
                ?: DifficultyStats()
            val updated = existing.copy(
                played = existing.played + 1,
                solved = maxOf(existing.solved, puzzlesSolved),
                bestScore = maxOf(existing.bestScore, score),
            )
            prefs[key] = json.encodeToString(updated)
        }
    }

    suspend fun clear(gameId: GameId) {
        dataStore.edit { prefs ->
            prefs.remove(statsKeyFor(gameId))
            prefs.remove(statsChallengerKeyFor(gameId))
        }
    }

    suspend fun clearAll() {
        dataStore.edit { prefs ->
            GameId.entries.forEach {
                prefs.remove(statsKeyFor(it))
                prefs.remove(statsChallengerKeyFor(it))
            }
        }
    }
}

private fun deriveGameStats(records: List<PlayRecord>): GameStats = GameStats(
    byDifficulty = records
        .filter { !it.isChallenger }
        .groupBy { it.difficulty }
        .mapValues { (_, entries) -> deriveDifficultyStats(entries) },
)

private fun deriveDifficultyStats(entries: List<PlayRecord>): DifficultyStats {
    val ordered = entries.sortedBy { it.timestampMillis }
    return DifficultyStats(
        played = ordered.size,
        solved = ordered.count { it.solved },
        bestScore = ordered.filter { it.solved }.maxOfOrNull { it.score } ?: 0,
        currentStreak = ordered.asReversed().takeWhile { it.solved }.size,
    )
}

private fun deriveChallengerStats(records: List<PlayRecord>): DifficultyStats {
    val runs = records.filter { it.isChallenger }
    return DifficultyStats(
        played = runs.size,
        solved = runs.maxOfOrNull { it.puzzlesSolved ?: 0 } ?: 0,
        bestScore = runs.maxOfOrNull { it.score } ?: 0,
        currentStreak = 0,
    )
}
