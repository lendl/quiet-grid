package com.quietgrid.app.games.sudoku

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SudokuPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val solutionGrid = listOf(
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
    private val givensGrid: List<List<Int?>> = solutionGrid.mapIndexed { r, row ->
        row.mapIndexed { c, value -> if (r == 0 && c == 0) null else value }
    }
    private val puzzleEntry = SudokuPuzzleEntry(
        id = "test-sudoku",
        difficulty = Difficulty.EASY.key,
        givens = givensGrid,
        solution = solutionGrid,
    )

    @Before
    fun setUp() {
        mockkObject(SudokuPuzzleBank)
        coEvery { SudokuPuzzleBank.randomPuzzle(any(), any(), any()) } returns puzzleEntry
    }

    @After
    fun tearDown() {
        unmockkObject(SudokuPuzzleBank)
    }

    private fun newViewModel(sessionStore: FakeSessionStore = FakeSessionStore(), statsStore: FakeStatsStore = FakeStatsStore()) =
        SudokuPlayViewModel(mockk<Context>(relaxed = true), sessionStore, statsStore, FakeHistoryStore(), Difficulty.EASY, resume = false)

    @Test
    fun `starting fresh loads the mocked puzzle with only the blank given empty`() {
        val viewModel = newViewModel()

        assertNull(viewModel.session?.board?.get(0)?.get(0))
        assertEquals(3, viewModel.session?.board?.get(0)?.get(1))
    }

    @Test
    fun `selecting the blank cell and entering its solution digit wins the puzzle`() {
        val sessionStore = FakeSessionStore()
        val viewModel = newViewModel(sessionStore)
        val results = mutableListOf<SudokuResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.onCellPress(0, 0)
        viewModel.onPressDigit(5)
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(1300)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals(1, results.size)
        assertEquals(true, results.single().solved)
        collectJob.cancel()
    }

    @Test
    fun `endPuzzle finalizes the session as an abandoned loss`() {
        val sessionStore = FakeSessionStore()
        val viewModel = newViewModel(sessionStore)
        val results = mutableListOf<SudokuResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endPuzzle()
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals("abandoned", results.single().lossReason)
        collectJob.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `toggling the hint computes asynchronously and ignores re-entrant taps while pending`() {
        val viewModel = newViewModel()
        viewModel.hintDispatcher = StandardTestDispatcher(mainDispatcherRule.dispatcher.scheduler)

        viewModel.toggleNextMoveHint()
        assertTrue(viewModel.isComputingHint)
        assertTrue(viewModel.nextMoveHintActive)
        assertNull(viewModel.nextMoveHint)

        viewModel.toggleNextMoveHint()
        assertTrue(viewModel.isComputingHint)

        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.isComputingHint)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `entering a digit while the hint is computing cancels it instead of applying a stale result`() {
        val viewModel = newViewModel()
        viewModel.hintDispatcher = StandardTestDispatcher(mainDispatcherRule.dispatcher.scheduler)

        viewModel.toggleNextMoveHint()
        assertTrue(viewModel.isComputingHint)

        viewModel.onCellPress(0, 0)
        viewModel.onPressDigit(1)
        assertFalse(viewModel.isComputingHint)
        assertFalse(viewModel.nextMoveHintActive)

        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.isComputingHint)
        assertNull(viewModel.nextMoveHint)
    }
}
