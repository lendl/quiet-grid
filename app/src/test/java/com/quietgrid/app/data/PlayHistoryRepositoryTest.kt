package com.quietgrid.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PlayHistoryRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `allRecords is empty when nothing was appended`() = runTest {
        val repository = PlayHistoryRepository(newDataStore(backgroundScope), FakePlayHistoryDao())

        assertTrue(repository.allRecords().first().isEmpty())
    }

    @Test
    fun `appendRecord adds one record and preserves its fields`() = runTest {
        val repository = PlayHistoryRepository(newDataStore(backgroundScope), FakePlayHistoryDao())
        val record = PlayRecord(
            gameId = GameId.SUDOKU.key,
            difficulty = Difficulty.HARD.key,
            puzzleId = "s9-123",
            solved = true,
            score = 42,
            elapsedSeconds = 120,
            timestampMillis = 1_000L,
            lossReason = null,
        )

        repository.appendRecord(record)

        val all = repository.allRecords().first()
        assertEquals(1, all.size)
        assertEquals(record, all.single())
    }

    @Test
    fun `appendRecord accumulates records in append order`() = runTest {
        val repository = PlayHistoryRepository(newDataStore(backgroundScope), FakePlayHistoryDao())
        val first = PlayRecord(GameId.SUDOKU.key, Difficulty.EASY.key, "s9-1", true, 10, 30, 1L)
        val second = PlayRecord(GameId.SUDOKU.key, Difficulty.EASY.key, "s9-2", false, 0, 45, 2L, "rule-failure")

        repository.appendRecord(first)
        repository.appendRecord(second)

        assertEquals(listOf(first, second), repository.allRecords().first())
    }

    @Test
    fun `recordsFor filters by game`() = runTest {
        val repository = PlayHistoryRepository(newDataStore(backgroundScope), FakePlayHistoryDao())
        repository.appendRecord(PlayRecord(GameId.SUDOKU.key, Difficulty.EASY.key, "s9-1", true, 10, 30, 1L))
        repository.appendRecord(PlayRecord(GameId.TAKUZU.key, Difficulty.EASY.key, "t6-1", true, 20, 40, 2L))

        val sudokuOnly = repository.recordsFor(GameId.SUDOKU).first()

        assertEquals(1, sudokuOnly.size)
        assertEquals(GameId.SUDOKU.key, sudokuOnly.single().gameId)
    }

    @Test
    fun `recordsForPuzzle filters by the full gameId, puzzleId, difficulty tuple`() = runTest {
        val repository = PlayHistoryRepository(newDataStore(backgroundScope), FakePlayHistoryDao())
        repository.appendRecord(PlayRecord(GameId.SUDOKU.key, Difficulty.HARD.key, "s9-shared", true, 10, 30, 1L))
        repository.appendRecord(PlayRecord(GameId.SUDOKU.key, Difficulty.MEDIUM.key, "s9-shared", true, 20, 40, 2L))

        val hardOnly = repository.recordsForPuzzle(GameId.SUDOKU, "s9-shared", Difficulty.HARD).first()

        assertEquals(1, hardOnly.size)
        assertEquals(Difficulty.HARD.key, hardOnly.single().difficulty)
    }

    @Test
    fun `clear empties allRecords`() = runTest {
        val repository = PlayHistoryRepository(newDataStore(backgroundScope), FakePlayHistoryDao())
        repository.appendRecord(PlayRecord(GameId.SUDOKU.key, Difficulty.EASY.key, "s9-1", true, 10, 30, 1L))

        repository.clear()

        assertTrue(repository.allRecords().first().isEmpty())
    }

    private fun newDataStore(scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { tempFolder.newFile("play_history.preferences_pb") },
    )
}

private class FakePlayHistoryDao : PlayHistoryDao {
    private val records = mutableListOf<PlayRecordEntity>()

    override fun allRecords(): Flow<List<PlayRecordEntity>> = flowOf(records.toList())

    override fun recordsFor(gameId: String): Flow<List<PlayRecordEntity>> =
        flowOf(records.filter { it.gameId == gameId })

    override fun recordsForPuzzle(gameId: String, puzzleId: String, difficulty: String): Flow<List<PlayRecordEntity>> =
        flowOf(records.filter { it.gameId == gameId && it.puzzleId == puzzleId && it.difficulty == difficulty })

    override suspend fun insert(record: PlayRecordEntity) {
        records.add(record)
    }

    override suspend fun insertAll(records: List<PlayRecordEntity>) {
        this.records.addAll(records)
    }

    override suspend fun clear() {
        records.clear()
    }
}
