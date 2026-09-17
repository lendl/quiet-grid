package com.quietgrid.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StatsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // Non-beta games: statsFor/challengerStatsFor derive from PlayHistoryStore, recordResult/recordChallengerResult are no-ops.

    @Test
    fun `statsFor derives played, solved, bestScore and streak from history for a non-beta game`() = runTest {
        val history = newHistoryStore(backgroundScope)
        val repository = StatsRepository(newDataStore(backgroundScope, "stats.preferences_pb"), history)

        history.appendRecord(PlayRecord(GameId.SUDOKU.key, Difficulty.EASY.key, "s9-1", true, 42, 30, 1L))

        val stats = repository.statsFor(GameId.SUDOKU).first().forDifficulty(Difficulty.EASY)
        assertEquals(1, stats.played)
        assertEquals(1, stats.solved)
        assertEquals(42, stats.bestScore)
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `statsFor resets currentStreak after a loss but keeps bestScore for a non-beta game`() = runTest {
        val history = newHistoryStore(backgroundScope)
        val repository = StatsRepository(newDataStore(backgroundScope, "stats.preferences_pb"), history)

        history.appendRecord(PlayRecord(GameId.SUDOKU.key, Difficulty.EASY.key, "s9-1", true, 42, 30, 1L))
        history.appendRecord(PlayRecord(GameId.SUDOKU.key, Difficulty.EASY.key, "s9-2", false, 0, 20, 2L, "abandoned"))

        val stats = repository.statsFor(GameId.SUDOKU).first().forDifficulty(Difficulty.EASY)
        assertEquals(2, stats.played)
        assertEquals(1, stats.solved)
        assertEquals(42, stats.bestScore)
        assertEquals(0, stats.currentStreak)
    }

    @Test
    fun `statsFor tracks difficulties independently for a non-beta game`() = runTest {
        val history = newHistoryStore(backgroundScope)
        val repository = StatsRepository(newDataStore(backgroundScope, "stats.preferences_pb"), history)

        history.appendRecord(PlayRecord(GameId.SUDOKU.key, Difficulty.EASY.key, "s9-1", true, 10, 30, 1L))
        history.appendRecord(PlayRecord(GameId.SUDOKU.key, Difficulty.HARD.key, "s9-2", true, 90, 60, 2L))

        val allStats = repository.statsFor(GameId.SUDOKU).first()
        assertEquals(10, allStats.forDifficulty(Difficulty.EASY).bestScore)
        assertEquals(90, allStats.forDifficulty(Difficulty.HARD).bestScore)
    }

    @Test
    fun `recordResult is a no-op for a non-beta game`() = runTest {
        val history = newHistoryStore(backgroundScope)
        val repository = StatsRepository(newDataStore(backgroundScope, "stats.preferences_pb"), history)

        repository.recordResult(GameId.SUDOKU, Difficulty.EASY, solved = true, score = 42)

        assertEquals(GameStats(), repository.statsFor(GameId.SUDOKU).first())
    }

    @Test
    fun `challengerStatsFor derives best puzzles-solved and best score from history for a non-beta game`() = runTest {
        val history = newHistoryStore(backgroundScope)
        val repository = StatsRepository(newDataStore(backgroundScope, "stats.preferences_pb"), history)

        history.appendRecord(challengerRecord(GameId.ANIMALDOKU, puzzlesSolved = 4, score = 900, timestamp = 1L))
        history.appendRecord(challengerRecord(GameId.ANIMALDOKU, puzzlesSolved = 7, score = 600, timestamp = 2L))

        val stats = repository.challengerStatsFor(GameId.ANIMALDOKU).first()
        assertEquals(2, stats.played)
        assertEquals(7, stats.solved)
        assertEquals(900, stats.bestScore)
    }

    @Test
    fun `recordChallengerResult is a no-op for a non-beta game`() = runTest {
        val history = newHistoryStore(backgroundScope)
        val repository = StatsRepository(newDataStore(backgroundScope, "stats.preferences_pb"), history)

        repository.recordChallengerResult(GameId.ANIMALDOKU, puzzlesSolved = 3, score = 300)

        assertEquals(DifficultyStats(), repository.challengerStatsFor(GameId.ANIMALDOKU).first())
    }

    @Test
    fun `statsForGames combines derived stats across multiple non-beta games`() = runTest {
        val history = newHistoryStore(backgroundScope)
        val repository = StatsRepository(newDataStore(backgroundScope, "stats.preferences_pb"), history)
        history.appendRecord(PlayRecord(GameId.SUDOKU.key, Difficulty.EASY.key, "s9-1", true, 10, 30, 1L))
        history.appendRecord(PlayRecord(GameId.TAKUZU.key, Difficulty.EASY.key, "t6-1", true, 20, 40, 2L))

        val combined = repository.statsForGames(listOf(GameId.SUDOKU, GameId.TAKUZU)).first()

        assertEquals(10, combined[GameId.SUDOKU]?.forDifficulty(Difficulty.EASY)?.bestScore)
        assertEquals(20, combined[GameId.TAKUZU]?.forDifficulty(Difficulty.EASY)?.bestScore)
    }

    // Beta games: statsFor/challengerStatsFor still read/write their own DataStore aggregate, unaffected by history.

    @Test
    fun `statsFor returns an empty GameStats when nothing was recorded for a beta game`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope, "stats.preferences_pb"), newHistoryStore(backgroundScope))

        assertEquals(GameStats(), repository.statsFor(GameId.BLOCKFILL).first())
    }

    @Test
    fun `recordResult increments played and solved on a win for a beta game`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope, "stats.preferences_pb"), newHistoryStore(backgroundScope))

        repository.recordResult(GameId.BLOCKFILL, Difficulty.EASY, solved = true, score = 42)

        val stats = repository.statsFor(GameId.BLOCKFILL).first().forDifficulty(Difficulty.EASY)
        assertEquals(1, stats.played)
        assertEquals(1, stats.solved)
        assertEquals(42, stats.bestScore)
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `recordResult on a loss increments played but resets streak without touching bestScore for a beta game`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope, "stats.preferences_pb"), newHistoryStore(backgroundScope))
        repository.recordResult(GameId.BLOCKFILL, Difficulty.EASY, solved = true, score = 42)

        repository.recordResult(GameId.BLOCKFILL, Difficulty.EASY, solved = false, score = 0)

        val stats = repository.statsFor(GameId.BLOCKFILL).first().forDifficulty(Difficulty.EASY)
        assertEquals(2, stats.played)
        assertEquals(1, stats.solved)
        assertEquals(42, stats.bestScore)
        assertEquals(0, stats.currentStreak)
    }

    @Test
    fun `clear removes stats for one beta game only`() = runTest {
        val dataStore = newDataStore(backgroundScope, "stats.preferences_pb")
        val repository = StatsRepository(dataStore, newHistoryStore(backgroundScope))
        repository.recordResult(GameId.BLOCKFILL, Difficulty.EASY, solved = true, score = 10)
        repository.recordResult(GameId.GAME_2048, Difficulty.EASY, solved = true, score = 20)

        repository.clear(GameId.BLOCKFILL)

        assertEquals(GameStats(), repository.statsFor(GameId.BLOCKFILL).first())
        assertEquals(20, repository.statsFor(GameId.GAME_2048).first().forDifficulty(Difficulty.EASY).bestScore)
    }

    @Test
    fun `clearAll removes stats for every beta game`() = runTest {
        val dataStore = newDataStore(backgroundScope, "stats.preferences_pb")
        val repository = StatsRepository(dataStore, newHistoryStore(backgroundScope))
        repository.recordResult(GameId.BLOCKFILL, Difficulty.EASY, solved = true, score = 10)
        repository.recordResult(GameId.GAME_2048, Difficulty.EASY, solved = true, score = 20)

        repository.clearAll()

        assertEquals(GameStats(), repository.statsFor(GameId.BLOCKFILL).first())
        assertEquals(GameStats(), repository.statsFor(GameId.GAME_2048).first())
    }

    @Test
    fun `recordChallengerResult tracks best puzzles-solved and best score across runs for a beta game`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope, "stats.preferences_pb"), newHistoryStore(backgroundScope))

        repository.recordChallengerResult(GameId.STARBATTLE, puzzlesSolved = 4, score = 900)
        repository.recordChallengerResult(GameId.STARBATTLE, puzzlesSolved = 7, score = 600)

        val stats = repository.challengerStatsFor(GameId.STARBATTLE).first()
        assertEquals(2, stats.played)
        assertEquals(7, stats.solved)
        assertEquals(900, stats.bestScore)
    }

    @Test
    fun `clear removes challenger stats for that beta game too`() = runTest {
        val dataStore = newDataStore(backgroundScope, "stats.preferences_pb")
        val repository = StatsRepository(dataStore, newHistoryStore(backgroundScope))
        repository.recordChallengerResult(GameId.STARBATTLE, puzzlesSolved = 3, score = 300)

        repository.clear(GameId.STARBATTLE)

        assertEquals(DifficultyStats(), repository.challengerStatsFor(GameId.STARBATTLE).first())
    }

    private fun challengerRecord(gameId: GameId, puzzlesSolved: Int, score: Int, timestamp: Long) = PlayRecord(
        gameId = gameId.key,
        difficulty = Difficulty.EASY.key,
        puzzleId = null,
        solved = true,
        score = score,
        elapsedSeconds = 60,
        timestampMillis = timestamp,
        lossReason = "time_up",
        isChallenger = true,
        puzzlesSolved = puzzlesSolved,
    )

    private fun newHistoryStore(scope: CoroutineScope): PlayHistoryStore =
        PlayHistoryRepository(newDataStore(scope, "history.preferences_pb"), FakePlayHistoryDao())

    private fun newDataStore(scope: CoroutineScope, fileName: String) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { tempFolder.newFile(fileName) },
    )
}
