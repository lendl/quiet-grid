package com.quietgrid.app.games.sudoku

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.session.AppForeground
import com.quietgrid.app.session.ChallengerResult
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val SOLVED_GRID = listOf(
    listOf(5, 3, 4, 6, 7, 8, 9, 1, 2),
    listOf(6, 7, 2, 1, 9, 5, 3, 4, 8),
    listOf(1, 9, 8, 3, 4, 2, 5, 6, 7),
    listOf(8, 5, 9, 7, 6, 1, 4, 2, 3),
    listOf(4, 2, 6, 8, 5, 3, 7, 9, 1),
    listOf(7, 1, 3, 9, 2, 4, 8, 5, 6),
    listOf(9, 6, 1, 5, 3, 7, 2, 8, 4),
    listOf(2, 8, 7, 4, 1, 9, 6, 3, 5),
    listOf(3, 4, 5, 2, 8, 6, 1, 7, 9),
)

private val testPuzzle = SudokuPuzzleEntry(
    id = "test",
    difficulty = "easy",
    givens = SOLVED_GRID.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r == 0 && c == 0) null else v } },
    solution = SOLVED_GRID,
)

private val twoEmptyPuzzle = SudokuPuzzleEntry(
    id = "two-empty",
    difficulty = "easy",
    givens = SOLVED_GRID.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r == 0 && c < 2) null else v } },
    solution = SOLVED_GRID,
)

private val fourEmptyPuzzle = SudokuPuzzleEntry(
    id = "four-empty",
    difficulty = "easy",
    givens = SOLVED_GRID.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r < 2 && c < 2) null else v } },
    solution = SOLVED_GRID,
)

class SudokuChallengerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkObject(SudokuPuzzleBank)
        coEvery { SudokuPuzzleBank.randomPuzzle(any(), any(), any()) } returns testPuzzle
    }

    @After
    fun tearDown() {
        unmockkObject(SudokuPuzzleBank)
    }

    private fun newViewModel(
        statsStore: FakeStatsStore = FakeStatsStore(),
        historyStore: FakeHistoryStore = FakeHistoryStore(),
        appForeground: AppForeground = AppForeground { true },
    ) =
        SudokuChallengerViewModel(mockk<Context>(relaxed = true), statsStore, historyStore, appForeground)

    @Test
    fun `starting fresh loads a puzzle on the Easy tier with starting lives and starting seconds`() {
        val viewModel = newViewModel()

        assertEquals(Difficulty.EASY, viewModel.session?.tier)
        assertEquals(SUDOKU_CHALLENGER_STARTING_LIVES, viewModel.session?.livesRemaining)
        assertEquals(SUDOKU_CHALLENGER_STARTING_SECONDS, viewModel.session?.secondsRemaining)
    }

    @Test
    fun `filling the last cell correctly solves the puzzle and advances with a bonus`() {
        val viewModel = newViewModel()

        viewModel.onCellPress(0, 0)
        viewModel.onPressDigit(5)

        val session = viewModel.session
        checkNotNull(session)
        assertEquals(1, session.puzzlesSolved)
        assertEquals(SUDOKU_CHALLENGER_STARTING_SECONDS + SUDOKU_CHALLENGER_BONUS_SECONDS, session.secondsRemaining, 0.0)
        assertEquals(SUDOKU_CHALLENGER_STARTING_LIVES, session.livesRemaining)
        assertEquals(1, session.puzzleHistory.size)
    }

    @Test
    fun `pressing a given cell does not select it`() {
        val viewModel = newViewModel()

        viewModel.onCellPress(0, 1)

        assertNull(viewModel.selectedCell)
    }

    @Test
    fun `note mode adds a note without placing a digit or costing a life`() {
        coEvery { SudokuPuzzleBank.randomPuzzle(any(), any(), any()) } returns twoEmptyPuzzle
        val viewModel = newViewModel()

        viewModel.onCellPress(0, 0)
        viewModel.onToggleNoteMode()
        viewModel.onPressDigit(7)

        val puzzleSession = checkNotNull(viewModel.session).puzzleSession
        assertNull(puzzleSession.board[0][0])
        assertEquals(setOf(7), puzzleSession.notes[0][0])
        assertEquals(SUDOKU_CHALLENGER_STARTING_LIVES, viewModel.session!!.livesRemaining)
    }

    @Test
    fun `pressing the digit a cell already holds clears it`() {
        coEvery { SudokuPuzzleBank.randomPuzzle(any(), any(), any()) } returns fourEmptyPuzzle
        val viewModel = newViewModel()

        viewModel.onCellPress(0, 0)
        viewModel.onPressDigit(5)
        viewModel.onPressDigit(5)

        assertNull(checkNotNull(viewModel.session).puzzleSession.board[0][0])
    }

    @Test
    fun `undo restores the previous board but keeps lives already lost`() {
        coEvery { SudokuPuzzleBank.randomPuzzle(any(), any(), any()) } returns twoEmptyPuzzle
        val viewModel = newViewModel()

        viewModel.onCellPress(0, 0)
        viewModel.onPressDigit(3)
        val afterWrong = checkNotNull(viewModel.session)
        assertEquals(SUDOKU_CHALLENGER_STARTING_LIVES - 1, afterWrong.livesRemaining)
        assertTrue(viewModel.canUndo)

        viewModel.onUndo()

        val afterUndo = checkNotNull(viewModel.session)
        assertNull(afterUndo.puzzleSession.board[0][0])
        assertEquals(SUDOKU_CHALLENGER_STARTING_LIVES - 1, afterUndo.livesRemaining)
        assertEquals(afterWrong.puzzleSession.accuracyDrops, afterUndo.puzzleSession.accuracyDrops)
        assertFalse(viewModel.canUndo)
    }

    @Test
    fun `auto candidates fill notes and stay on for the next puzzle`() {
        coEvery { SudokuPuzzleBank.randomPuzzle(any(), any(), any()) } returns twoEmptyPuzzle
        val viewModel = newViewModel()

        viewModel.onToggleAutoCandidateMode()
        assertEquals(setOf(5), checkNotNull(viewModel.session).puzzleSession.notes[0][0])

        viewModel.onCellPress(0, 0)
        viewModel.onPressDigit(5)
        viewModel.onCellPress(0, 1)
        viewModel.onPressDigit(3)

        val next = checkNotNull(viewModel.session)
        assertEquals(1, next.puzzlesSolved)
        assertTrue(next.puzzleSession.autoCandidateMode)
        assertEquals(setOf(5), next.puzzleSession.notes[0][0])
    }

    @Test
    fun `loading the next puzzle clears the selection and undo history`() {
        val viewModel = newViewModel()

        viewModel.onCellPress(0, 0)
        viewModel.onPressDigit(5)

        assertEquals(1, checkNotNull(viewModel.session).puzzlesSolved)
        assertNull(viewModel.selectedCell)
        assertFalse(viewModel.canUndo)
    }

    @Test
    fun `filling the last cell wrong invalidates every touched unit at once and exhausts lives`() {
        val statsStore = FakeStatsStore()
        val viewModel = newViewModel(statsStore)
        val results = mutableListOf<ChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.onCellPress(0, 0)
        viewModel.onPressDigit(9)

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, results.size)
        val result = results.single()
        assertEquals("lives_exhausted", result.reason)
        assertEquals(0, result.puzzlesSolved)

        val recorded = runBlocking { statsStore.challengerStatsFor(GameId.SUDOKU).first() }
        assertEquals(1, recorded.played)
        assertEquals(0, recorded.solved)

        collectJob.cancel()
    }

    @Test
    fun `endRun finalizes the run as abandoned and emits exactly once`() {
        val viewModel = newViewModel()
        val results = mutableListOf<ChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endRun()

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, results.size)
        assertEquals("abandoned", results.single().reason)

        viewModel.endRun()
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        assertEquals(1, results.size)

        collectJob.cancel()
    }

    @Test
    fun `the ticker counting down to zero finalizes the run as time_up`() {
        val viewModel = newViewModel()
        val results = mutableListOf<ChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(SUDOKU_CHALLENGER_STARTING_SECONDS.toLong() * 1000L + 500L)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, results.size)
        assertEquals("time_up", results.single().reason)

        collectJob.cancel()
    }

    @Test
    fun `finalizeRun on abandoned includes the previous best score`() {
        val statsStore = FakeStatsStore()
        statsStore.seedChallenger(GameId.SUDOKU, solved = 2, bestScore = 500)
        val viewModel = newViewModel(statsStore)
        val results = mutableListOf<ChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endRun()

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(500, results.single().previousBest)

        collectJob.cancel()
    }

    @Test
    fun `finalizeRun appends a Challenger play record`() {
        val historyStore = FakeHistoryStore()
        val viewModel = newViewModel(historyStore = historyStore)

        viewModel.endRun()

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        val record = historyStore.appended.single()
        assertEquals(GameId.SUDOKU.key, record.gameId)
        assertEquals(true, record.isChallenger)
        assertEquals(0, record.puzzlesSolved)
        assertEquals("abandoned", record.lossReason)
    }

    @Test
    fun `the ticker does not count down while the app is backgrounded`() {
        val viewModel = newViewModel(appForeground = { false })

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(SUDOKU_CHALLENGER_STARTING_SECONDS.toLong() * 1000L + 500L)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(SUDOKU_CHALLENGER_STARTING_SECONDS, viewModel.session!!.secondsRemaining, 0.0)
    }
}
