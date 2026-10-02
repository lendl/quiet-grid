package com.quietgrid.app.session

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeStatsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private data class TestRun(
    override val tier: Difficulty = Difficulty.MEDIUM,
    override val solvesInTier: Int = 1,
    override val puzzlesSolved: Int = 4,
    override val score: Int = 900,
    override val secondsRemaining: Double = 5.0,
    override val fastestSolveSeconds: Double? = 12.0,
    override val puzzleHistory: List<ChallengerPuzzleSolve> = listOf(ChallengerPuzzleSolve(Difficulty.EASY, 30.0)),
) : ChallengerRunState

class ChallengerRunControllerTest {

    private fun TestScope.newController(
        statsStore: FakeStatsStore = FakeStatsStore(),
        historyStore: FakeHistoryStore = FakeHistoryStore(),
        appForeground: AppForeground = AppForeground { true },
    ) = ChallengerRunController<TestRun>(
        scope = backgroundScope,
        gameId = GameId.SUDOKU,
        statsStore = statsStore,
        historyStore = historyStore,
        appForeground = appForeground,
        tick = { it.copy(secondsRemaining = it.secondsRemaining - 1.0) },
    )

    @Test
    fun `ticker counts down once per second while the app is in the foreground`() = runTest {
        val controller = newController()

        controller.start(TestRun())
        advanceTimeBy(3_001)

        assertEquals(2.0, controller.session!!.secondsRemaining, 0.0)
    }

    @Test
    fun `ticker holds while the app is backgrounded and resumes on return`() = runTest {
        var foreground = false
        val controller = newController(appForeground = { foreground })

        controller.start(TestRun())
        advanceTimeBy(10_001)

        assertEquals(5.0, controller.session!!.secondsRemaining, 0.0)
        assertFalse(controller.isFinalized)

        foreground = true
        advanceTimeBy(2_000)

        assertEquals(3.0, controller.session!!.secondsRemaining, 0.0)
    }

    @Test
    fun `reaching zero seconds finalizes the run as time_up`() = runTest {
        val controller = newController()
        val results = mutableListOf<ChallengerResult>()
        backgroundScope.launch { controller.result.collect { results.add(it) } }

        controller.start(TestRun())
        advanceTimeBy(5_001 + 450)
        runCurrent()

        assertTrue(controller.isFinalized)
        assertEquals("time_up", results.single().reason)
    }

    @Test
    fun `endRun records stats and history once and emits an abandoned result`() = runTest {
        val statsStore = FakeStatsStore()
        statsStore.seedChallenger(GameId.SUDOKU, solved = 2, bestScore = 500)
        val historyStore = FakeHistoryStore()
        val controller = newController(statsStore, historyStore)
        val results = mutableListOf<ChallengerResult>()
        backgroundScope.launch { controller.result.collect { results.add(it) } }

        controller.start(TestRun())
        controller.endRun()
        controller.endRun()
        advanceTimeBy(500)
        runCurrent()

        val expected = ChallengerResult(
            puzzlesSolved = 4,
            tierReached = Difficulty.MEDIUM,
            score = 900,
            isNewHighScore = true,
            reason = "abandoned",
            previousBest = 500,
            fastestSolveSeconds = 12.0,
            puzzleHistory = listOf(ChallengerPuzzleSolve(Difficulty.EASY, 30.0)),
            solvesInTier = 1,
        )
        assertEquals(listOf(expected), results)
        assertEquals(2, statsStore.challengerStatsFor(GameId.SUDOKU).first().played)
        val record = historyStore.appended.single()
        assertEquals(GameId.SUDOKU.key, record.gameId)
        assertEquals(Difficulty.MEDIUM.key, record.difficulty)
        assertEquals(30, record.elapsedSeconds)
        assertEquals("abandoned", record.lossReason)
        assertTrue(record.isChallenger)
    }
}
