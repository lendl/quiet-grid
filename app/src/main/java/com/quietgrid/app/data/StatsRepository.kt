package com.quietgrid.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

private val json = Json { ignoreUnknownKeys = true }

class StatsRepository(private val context: Context) {
    private fun keyFor(gameId: GameId) = stringPreferencesKey("stats_${gameId.key}")

    fun statsForGames(gameIds: List<GameId>): Flow<Map<GameId, GameStats>> =
        combine(gameIds.map { id -> statsFor(id).map { id to it } }) { pairs -> pairs.toMap() }

    fun statsFor(gameId: GameId): Flow<GameStats> = context.appDataStore.data.map { prefs ->
        prefs[keyFor(gameId)]?.let { raw ->
            runCatching { json.decodeFromString<GameStats>(raw) }.getOrNull()
        } ?: GameStats()
    }

    suspend fun recordResult(gameId: GameId, difficulty: Difficulty, solved: Boolean, score: Int) {
        context.appDataStore.edit { prefs ->
            val key = keyFor(gameId)
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

    suspend fun clear(gameId: GameId) {
        context.appDataStore.edit { prefs -> prefs.remove(keyFor(gameId)) }
    }

    suspend fun clearAll() {
        context.appDataStore.edit { prefs ->
            GameId.entries.forEach { prefs.remove(keyFor(it)) }
        }
    }
}
