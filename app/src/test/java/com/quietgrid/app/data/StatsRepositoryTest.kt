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

    @Test
    fun `statsFor returns an empty GameStats when nothing was recorded`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope))

        assertEquals(GameStats(), repository.statsFor(GameId.SUDOKU).first())
    }

    @Test
    fun `recordResult increments played and solved on a win`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope))

        repository.recordResult(GameId.SUDOKU, Difficulty.EASY, solved = true, score = 42)

        val stats = repository.statsFor(GameId.SUDOKU).first().forDifficulty(Difficulty.EASY)
        assertEquals(1, stats.played)
        assertEquals(1, stats.solved)
        assertEquals(42, stats.bestScore)
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `recordResult on a loss increments played but resets streak without touching bestScore`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope))
        repository.recordResult(GameId.SUDOKU, Difficulty.EASY, solved = true, score = 42)

        repository.recordResult(GameId.SUDOKU, Difficulty.EASY, solved = false, score = 0)

        val stats = repository.statsFor(GameId.SUDOKU).first().forDifficulty(Difficulty.EASY)
        assertEquals(2, stats.played)
        assertEquals(1, stats.solved)
        assertEquals(42, stats.bestScore)
        assertEquals(0, stats.currentStreak)
    }

    @Test
    fun `bestScore only rises when a higher-scoring win is recorded`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope))
        repository.recordResult(GameId.SUDOKU, Difficulty.EASY, solved = true, score = 50)

        repository.recordResult(GameId.SUDOKU, Difficulty.EASY, solved = true, score = 30)

        val stats = repository.statsFor(GameId.SUDOKU).first().forDifficulty(Difficulty.EASY)
        assertEquals(50, stats.bestScore)
        assertEquals(2, stats.currentStreak)
    }

    @Test
    fun `stats are tracked independently per difficulty`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope))
        repository.recordResult(GameId.SUDOKU, Difficulty.EASY, solved = true, score = 10)
        repository.recordResult(GameId.SUDOKU, Difficulty.HARD, solved = true, score = 90)

        val allStats = repository.statsFor(GameId.SUDOKU).first()
        assertEquals(10, allStats.forDifficulty(Difficulty.EASY).bestScore)
        assertEquals(90, allStats.forDifficulty(Difficulty.HARD).bestScore)
    }

    @Test
    fun `clear removes stats for one game only`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope))
        repository.recordResult(GameId.SUDOKU, Difficulty.EASY, solved = true, score = 10)
        repository.recordResult(GameId.TAKUZU, Difficulty.EASY, solved = true, score = 20)

        repository.clear(GameId.SUDOKU)

        assertEquals(GameStats(), repository.statsFor(GameId.SUDOKU).first())
        assertEquals(20, repository.statsFor(GameId.TAKUZU).first().forDifficulty(Difficulty.EASY).bestScore)
    }

    @Test
    fun `clearAll removes stats for every game`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope))
        repository.recordResult(GameId.SUDOKU, Difficulty.EASY, solved = true, score = 10)
        repository.recordResult(GameId.TAKUZU, Difficulty.EASY, solved = true, score = 20)

        repository.clearAll()

        assertEquals(GameStats(), repository.statsFor(GameId.SUDOKU).first())
        assertEquals(GameStats(), repository.statsFor(GameId.TAKUZU).first())
    }

    @Test
    fun `statsForGames combines stats across multiple games`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope))
        repository.recordResult(GameId.SUDOKU, Difficulty.EASY, solved = true, score = 10)
        repository.recordResult(GameId.TAKUZU, Difficulty.EASY, solved = true, score = 20)

        val combined = repository.statsForGames(listOf(GameId.SUDOKU, GameId.TAKUZU)).first()

        assertEquals(10, combined[GameId.SUDOKU]?.forDifficulty(Difficulty.EASY)?.bestScore)
        assertEquals(20, combined[GameId.TAKUZU]?.forDifficulty(Difficulty.EASY)?.bestScore)
    }

    @Test
    fun `challengerStatsFor returns empty DifficultyStats when nothing was recorded`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope))

        assertEquals(DifficultyStats(), repository.challengerStatsFor(GameId.ANIMALDOKU).first())
    }

    @Test
    fun `recordChallengerResult tracks best puzzles-solved and best score across runs`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope))

        repository.recordChallengerResult(GameId.ANIMALDOKU, puzzlesSolved = 4, score = 900)
        repository.recordChallengerResult(GameId.ANIMALDOKU, puzzlesSolved = 7, score = 600)

        val stats = repository.challengerStatsFor(GameId.ANIMALDOKU).first()
        assertEquals(2, stats.played)
        assertEquals(7, stats.solved)
        assertEquals(900, stats.bestScore)
    }

    @Test
    fun `challenger stats are independent from normal per-difficulty stats`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope))
        repository.recordResult(GameId.ANIMALDOKU, Difficulty.EASY, solved = true, score = 500)

        repository.recordChallengerResult(GameId.ANIMALDOKU, puzzlesSolved = 3, score = 300)

        assertEquals(500, repository.statsFor(GameId.ANIMALDOKU).first().forDifficulty(Difficulty.EASY).bestScore)
        assertEquals(300, repository.challengerStatsFor(GameId.ANIMALDOKU).first().bestScore)
    }

    @Test
    fun `clear removes challenger stats for that game too`() = runTest {
        val repository = StatsRepository(newDataStore(backgroundScope))
        repository.recordChallengerResult(GameId.ANIMALDOKU, puzzlesSolved = 3, score = 300)

        repository.clear(GameId.ANIMALDOKU)

        assertEquals(DifficultyStats(), repository.challengerStatsFor(GameId.ANIMALDOKU).first())
    }

    private fun newDataStore(scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { tempFolder.newFile("stats.preferences_pb") },
    )
}
