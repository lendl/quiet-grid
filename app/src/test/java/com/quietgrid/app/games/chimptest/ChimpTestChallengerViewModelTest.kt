package com.quietgrid.app.games.chimptest

import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.GameId
import com.quietgrid.app.testutil.FakeStatsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ChimpTestChallengerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `finalizeRun on abandoned includes the previous best score and no fastest solve`() {
        val statsStore = FakeStatsStore()
        statsStore.seedChallenger(GameId.CHIMPTEST, solved = 2, bestScore = 500)
        val viewModel = ChimpTestChallengerViewModel(statsStore)
        val results = mutableListOf<ChimpTestChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endRun()

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        val result = results.single()
        assertEquals(500, result.previousBest)
        assertNull(result.fastestSolveSeconds)
        assertEquals(emptyList<ChallengerPuzzleSolve>(), result.puzzleHistory)

        collectJob.cancel()
    }
}
