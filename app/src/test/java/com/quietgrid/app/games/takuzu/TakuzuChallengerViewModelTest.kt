package com.quietgrid.app.games.takuzu

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry
import com.quietgrid.engine.takuzu.gridToHex
import com.quietgrid.engine.takuzu.maskToHex
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
import org.junit.Rule
import org.junit.Test

private val SOLVABLE_GRID = listOf(
    listOf(0, 1, 0, 1),
    listOf(1, 0, 1, 0),
    listOf(0, 1, 1, 0),
    listOf(1, 0, 0, 1),
)
private val SINGLE_BLANK_MASK = listOf(
    listOf(false, true, true, true),
    listOf(true, true, true, true),
    listOf(true, true, true, true),
    listOf(true, true, true, true),
)

private val solvableTestPuzzle = TakuzuPuzzleEntry(
    id = "solvable",
    size = 4,
    difficulty = "easy",
    solution = gridToHex(SOLVABLE_GRID),
    mask = maskToHex(SINGLE_BLANK_MASK),
)

private val losingTestPuzzle = TakuzuPuzzleEntry(
    id = "losing",
    size = 4,
    difficulty = "easy",
    solution = gridToHex(listOf(listOf(1, 1, 0, 1), listOf(1, 0, 1, 0), listOf(0, 1, 1, 0), listOf(1, 0, 0, 1))),
    mask = maskToHex(SINGLE_BLANK_MASK),
)

class TakuzuChallengerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @After
    fun tearDown() {
        unmockkObject(TakuzuPuzzleBank)
    }

    private fun newViewModel(
        puzzle: TakuzuPuzzleEntry,
        statsStore: FakeStatsStore = FakeStatsStore(),
        historyStore: FakeHistoryStore = FakeHistoryStore(),
    ): TakuzuChallengerViewModel {
        mockkObject(TakuzuPuzzleBank)
        coEvery { TakuzuPuzzleBank.randomPuzzle(any(), any(), any()) } returns puzzle
        return TakuzuChallengerViewModel(mockk<Context>(relaxed = true), statsStore, historyStore)
    }

    @Test
    fun `starting fresh loads a puzzle on the Easy tier with starting lives and starting seconds`() {
        val viewModel = newViewModel(solvableTestPuzzle)

        assertEquals(Difficulty.EASY, viewModel.session?.tier)
        assertEquals(TAKUZU_CHALLENGER_STARTING_LIVES, viewModel.session?.livesRemaining)
        assertEquals(TAKUZU_CHALLENGER_STARTING_SECONDS, viewModel.session?.secondsRemaining)
    }

    @Test
    fun `pressing the last cell to the correct value solves the puzzle and advances with a bonus`() {
        val viewModel = newViewModel(solvableTestPuzzle)

        viewModel.onCellPress(0, 0)

        val session = viewModel.session
        checkNotNull(session)
        assertEquals(1, session.puzzlesSolved)
        assertEquals(TAKUZU_CHALLENGER_STARTING_SECONDS + TAKUZU_CHALLENGER_BONUS_SECONDS, session.secondsRemaining, 0.0)
        assertEquals(TAKUZU_CHALLENGER_STARTING_LIVES, session.livesRemaining)
        assertEquals(1, session.puzzleHistory.size)
    }

    @Test
    fun `pressing the last cell to a wrong value costs both touched lines worth of lives without exhausting them`() {
        val statsStore = FakeStatsStore()
        val viewModel = newViewModel(losingTestPuzzle, statsStore)

        viewModel.onCellPress(0, 0)

        val session = viewModel.session
        checkNotNull(session)
        assertEquals(TAKUZU_CHALLENGER_STARTING_LIVES - 2, session.livesRemaining)
        assertEquals(0, session.puzzlesSolved)
    }

    @Test
    fun `endRun finalizes the run as abandoned and emits exactly once`() {
        val viewModel = newViewModel(solvableTestPuzzle)
        val results = mutableListOf<TakuzuChallengerResult>()
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
        val viewModel = newViewModel(solvableTestPuzzle)
        val results = mutableListOf<TakuzuChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(TAKUZU_CHALLENGER_STARTING_SECONDS.toLong() * 1000L + 500L)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, results.size)
        assertEquals("time_up", results.single().reason)

        collectJob.cancel()
    }

    @Test
    fun `finalizeRun on abandoned includes the previous best score`() {
        val statsStore = FakeStatsStore()
        statsStore.seedChallenger(GameId.TAKUZU, solved = 2, bestScore = 500)
        val viewModel = newViewModel(solvableTestPuzzle, statsStore)
        val results = mutableListOf<TakuzuChallengerResult>()
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
        val viewModel = newViewModel(solvableTestPuzzle, historyStore = historyStore)

        viewModel.endRun()

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        val record = historyStore.appended.single()
        assertEquals(GameId.TAKUZU.key, record.gameId)
        assertEquals(true, record.isChallenger)
        assertEquals(0, record.puzzlesSolved)
        assertEquals("abandoned", record.lossReason)
    }
}
