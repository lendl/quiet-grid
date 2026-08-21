package com.quietgrid.app.testutil

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.GameStats
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSessionStore : SessionStore {
    private val state = MutableStateFlow<ActiveSessionEnvelope?>(null)
    override val activeSession: Flow<ActiveSessionEnvelope?> = state
    var saveCount = 0
        private set
    var cleared = false
        private set

    fun preload(envelope: ActiveSessionEnvelope) {
        state.value = envelope
    }

    override suspend fun save(envelope: ActiveSessionEnvelope) {
        saveCount++
        state.value = envelope
    }

    override suspend fun clear() {
        cleared = true
        state.value = null
    }
}

class FakeStatsStore : StatsStore {
    private val stats = mutableMapOf<GameId, GameStats>()

    fun seed(gameId: GameId, difficulty: Difficulty, solved: Int, bestScore: Int) {
        stats[gameId] = GameStats(
            byDifficulty = mapOf(difficulty.key to com.quietgrid.app.data.DifficultyStats(played = solved, solved = solved, bestScore = bestScore)),
        )
    }

    override fun statsFor(gameId: GameId): Flow<GameStats> = MutableStateFlow(stats[gameId] ?: GameStats())

    override suspend fun recordResult(gameId: GameId, difficulty: Difficulty, solved: Boolean, score: Int) {
        val current = stats[gameId] ?: GameStats()
        val existing = current.forDifficulty(difficulty)
        val updated = existing.copy(
            played = existing.played + 1,
            solved = existing.solved + if (solved) 1 else 0,
            bestScore = if (solved) maxOf(existing.bestScore, score) else existing.bestScore,
        )
        stats[gameId] = GameStats(byDifficulty = current.byDifficulty + (difficulty.key to updated))
    }
}

class FakeHistoryStore : PlayHistoryStore {
    private val records = mutableListOf<PlayRecord>()
    val appended: List<PlayRecord> get() = records

    override fun allRecords(): Flow<List<PlayRecord>> = MutableStateFlow(records.toList())

    override fun recordsFor(gameId: GameId): Flow<List<PlayRecord>> =
        MutableStateFlow(records.filter { it.gameId == gameId.key })

    override fun recordsForPuzzle(gameId: GameId, puzzleId: String, difficulty: Difficulty): Flow<List<PlayRecord>> =
        MutableStateFlow(records.filter { it.gameId == gameId.key && it.puzzleId == puzzleId && it.difficulty == difficulty.key })

    override suspend fun appendRecord(record: PlayRecord) {
        records.add(record)
    }

    override suspend fun clear() {
        records.clear()
    }
}
