package com.quietgrid.app.games.starbattle

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry
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

private val testPuzzle = StarBattlePuzzleEntry(
    id = "test",
    size = 5,
    difficulty = "easy",
    k = 1,
    regions = List(5) { row -> List(5) { row } },
    solution = List(5) { row -> listOf(row) },
)

class StarBattleChallengerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkObject(StarBattlePuzzleBank)
        coEvery { StarBattlePuzzleBank.randomPuzzle(any(), any(), any()) } returns testPuzzle
    }

    @After
    fun tearDown() {
        unmockkObject(StarBattlePuzzleBank)
    }

    private fun newViewModel(statsStore: FakeStatsStore = FakeStatsStore()) =
        StarBattleChallengerViewModel(mockk<Context>(relaxed = true), statsStore)

    private fun solveCurrentPuzzle(viewModel: StarBattleChallengerViewModel) {
        for (row in testPuzzle.solution.indices) {
            viewModel.onCellDoubleTap(row, testPuzzle.solution[row].first())
        }
    }

    @Test
    fun `starting fresh loads a puzzle on the Easy tier with starting lives and starting seconds`() {
        val viewModel = newViewModel()

        assertEquals(Difficulty.EASY, viewModel.session?.tier)
        assertEquals(STARBATTLE_STARTING_LIVES, viewModel.session?.puzzleSession?.lives)
        assertEquals(STARBATTLE_CHALLENGER_STARTING_SECONDS, viewModel.session?.secondsRemaining)
    }

    @Test
    fun `solving a puzzle advances to the next puzzle with a bonus and carried-over lives`() {
        val viewModel = newViewModel()

        solveCurrentPuzzle(viewModel)

        val session = viewModel.session
        checkNotNull(session)
        assertEquals(1, session.puzzlesSolved)
        assertEquals(STARBATTLE_CHALLENGER_STARTING_SECONDS + STARBATTLE_CHALLENGER_BONUS_SECONDS, session.secondsRemaining, 0.0)
        assertEquals(STARBATTLE_STARTING_LIVES, session.puzzleSession.lives)
        assertEquals(StarBattleStatus.PLAYING, session.puzzleSession.status)
        assertEquals(1, session.puzzleHistory.size)
    }

    @Test
    fun `losing all shared lives finalizes the run and records challenger stats`() {
        val statsStore = FakeStatsStore()
        val viewModel = newViewModel(statsStore)
        val results = mutableListOf<StarBattleChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        solveCurrentPuzzle(viewModel)

        viewModel.onCellDoubleTap(0, 1)
        viewModel.onCellDoubleTap(1, 0)
        viewModel.onCellDoubleTap(2, 0)

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, results.size)
        val result = results.single()
        assertEquals("hearts_exhausted", result.reason)
        assertEquals(1, result.puzzlesSolved)

        val recorded = runBlocking { statsStore.challengerStatsFor(GameId.STARBATTLE).first() }
        assertEquals(1, recorded.played)
        assertEquals(1, recorded.solved)
        assertTrue(recorded.bestScore > 0)

        collectJob.cancel()
    }

    @Test
    fun `endRun finalizes the run as abandoned and emits exactly once`() {
        val viewModel = newViewModel()
        val results = mutableListOf<StarBattleChallengerResult>()
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
        val results = mutableListOf<StarBattleChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(STARBATTLE_CHALLENGER_STARTING_SECONDS.toLong() * 1000L + 500L)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, results.size)
        assertEquals("time_up", results.single().reason)

        collectJob.cancel()
    }
}
