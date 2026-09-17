package com.quietgrid.app.games.nonogram

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val testPuzzle = NonogramPuzzleEntry(
    id = "test",
    difficulty = "easy",
    rows = 2,
    cols = 2,
    solution = listOf(listOf(true, false), listOf(false, true)),
)

class NonogramChallengerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkObject(NonogramPuzzleBank)
        coEvery { NonogramPuzzleBank.randomPuzzle(any(), any(), any()) } returns testPuzzle
    }

    @After
    fun tearDown() {
        unmockkObject(NonogramPuzzleBank)
    }

    private fun newViewModel(statsStore: FakeStatsStore = FakeStatsStore()) =
        NonogramChallengerViewModel(mockk<Context>(relaxed = true), statsStore)

    @Test
    fun `starting fresh loads a puzzle on the Easy tier with starting lives and starting seconds`() {
        val viewModel = newViewModel()

        assertEquals(Difficulty.EASY, viewModel.session?.tier)
        assertEquals(NONOGRAM_CHALLENGER_STARTING_LIVES, viewModel.session?.livesRemaining)
        assertEquals(NONOGRAM_CHALLENGER_STARTING_SECONDS, viewModel.session?.secondsRemaining)
    }

    @Test
    fun `filling both correct cells solves the puzzle and advances with a bonus`() {
        val viewModel = newViewModel()

        viewModel.onCellTap(0, 0)
        viewModel.onCellTap(1, 1)

        val session = viewModel.session
        checkNotNull(session)
        assertEquals(1, session.puzzlesSolved)
        assertEquals(NONOGRAM_CHALLENGER_STARTING_SECONDS + NONOGRAM_CHALLENGER_BONUS_SECONDS, session.secondsRemaining, 0.0)
        assertEquals(NONOGRAM_CHALLENGER_STARTING_LIVES, session.livesRemaining)
        assertEquals(1, session.puzzleHistory.size)
    }

    @Test
    fun `tapping a cell the solution marks blank costs a life without applying the fill`() {
        val viewModel = newViewModel()

        viewModel.onCellTap(0, 1)

        val session = viewModel.session
        checkNotNull(session)
        assertEquals(NONOGRAM_CHALLENGER_STARTING_LIVES - 1, session.livesRemaining)
        assertEquals(null, session.puzzleSession.board[0][1])
        assertEquals(0, session.puzzlesSolved)
    }

    @Test
    fun `three wrong taps on the same cell exhaust all lives and finalize the run`() {
        val statsStore = FakeStatsStore()
        val viewModel = newViewModel(statsStore)
        val results = mutableListOf<NonogramChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.onCellTap(0, 1)
        viewModel.onCellTap(0, 1)
        viewModel.onCellTap(0, 1)

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, results.size)
        assertEquals("lives_exhausted", results.single().reason)

        val recorded = runBlocking { statsStore.challengerStatsFor(GameId.NONOGRAM).first() }
        assertEquals(1, recorded.played)
        assertEquals(0, recorded.solved)

        collectJob.cancel()
    }

    @Test
    fun `endRun finalizes the run as abandoned and emits exactly once`() {
        val viewModel = newViewModel()
        val results = mutableListOf<NonogramChallengerResult>()
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
        val results = mutableListOf<NonogramChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(NONOGRAM_CHALLENGER_STARTING_SECONDS.toLong() * 1000L + 500L)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, results.size)
        assertEquals("time_up", results.single().reason)

        collectJob.cancel()
    }

    @Test
    fun `finalizeRun on abandoned includes the previous best score`() {
        val statsStore = FakeStatsStore()
        statsStore.seedChallenger(GameId.NONOGRAM, solved = 2, bestScore = 500)
        val viewModel = newViewModel(statsStore)
        val results = mutableListOf<NonogramChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endRun()

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(500, results.single().previousBest)

        collectJob.cancel()
    }
}
