package com.quietgrid.app.games.animaldoku

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val testPuzzle = AnimalDokuPuzzleEntry(
    id = "test",
    size = 5,
    difficulty = "easy",
    regions = List(5) { row -> List(5) { row } },
    solution = listOf(0, 1, 2, 3, 4),
)

class AnimalDokuChallengerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkObject(AnimalDokuPuzzleBank)
        coEvery { AnimalDokuPuzzleBank.randomPuzzle(any(), any(), any()) } returns testPuzzle
    }

    @After
    fun tearDown() {
        unmockkObject(AnimalDokuPuzzleBank)
    }

    private fun newViewModel(statsStore: FakeStatsStore = FakeStatsStore()) =
        AnimalDokuChallengerViewModel(mockk<Context>(relaxed = true), statsStore)

    private fun solveCurrentPuzzle(viewModel: AnimalDokuChallengerViewModel) {
        for (row in testPuzzle.solution.indices) {
            viewModel.onCellDoubleTap(row, testPuzzle.solution[row])
        }
    }

    @Test
    fun `starting fresh loads a puzzle on the Easy tier with starting lives and starting seconds`() {
        val viewModel = newViewModel()

        assertEquals(Difficulty.EASY, viewModel.session?.tier)
        assertEquals(ANIMALDOKU_STARTING_LIVES, viewModel.session?.puzzleSession?.lives)
        assertEquals(ANIMALDOKU_CHALLENGER_STARTING_SECONDS, viewModel.session?.secondsRemaining)
    }

    @Test
    fun `solving a puzzle advances to the next puzzle with a bonus and carried-over lives`() {
        val viewModel = newViewModel()

        solveCurrentPuzzle(viewModel)

        val session = viewModel.session
        checkNotNull(session)
        assertEquals(1, session.puzzlesSolved)
        assertEquals(ANIMALDOKU_CHALLENGER_STARTING_SECONDS + ANIMALDOKU_CHALLENGER_BONUS_SECONDS, session.secondsRemaining, 0.0)
        assertEquals(ANIMALDOKU_STARTING_LIVES, session.puzzleSession.lives)
        assertEquals(AnimalDokuStatus.PLAYING, session.puzzleSession.status)
        assertEquals(AnimalDokuCellState.EMPTY, session.puzzleSession.cells[0][0])
        assertEquals(AnimalDokuCellState.EMPTY, session.puzzleSession.cells[4][4])
    }

    @Test
    fun `losing all shared lives finalizes the run and records challenger stats`() {
        val statsStore = FakeStatsStore()
        val viewModel = newViewModel(statsStore)
        val results = mutableListOf<AnimalDokuChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        solveCurrentPuzzle(viewModel)

        viewModel.onCellDoubleTap(0, 1)
        viewModel.onCellDoubleTap(1, 0)
        viewModel.onCellDoubleTap(2, 0)

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, results.size)
        val result = results.single()
        assertEquals("lives_exhausted", result.reason)
        assertEquals(1, result.puzzlesSolved)

        val recorded = runBlocking { statsStore.challengerStatsFor(GameId.ANIMALDOKU).first() }
        assertEquals(1, recorded.played)
        assertEquals(1, recorded.solved)
        assertTrue(recorded.bestScore > 0)

        collectJob.cancel()
    }

    @Test
    fun `endRun finalizes the run as abandoned and emits exactly once`() {
        val viewModel = newViewModel()
        val results = mutableListOf<AnimalDokuChallengerResult>()
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
        val results = mutableListOf<AnimalDokuChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(ANIMALDOKU_CHALLENGER_STARTING_SECONDS.toLong() * 1000L + 500L)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, results.size)
        assertEquals("time_up", results.single().reason)

        collectJob.cancel()
    }
}
