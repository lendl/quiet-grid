package com.quietgrid.app.games.chimptest

import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChimpTestPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starting fresh creates a playable session with zero elapsed seconds`() {
        val viewModel = ChimpTestPlayViewModel(FakeSessionStore(), FakeStatsStore(), Difficulty.EASY, resume = false)

        assertNotNull(viewModel.session)
        assertEquals(ChimpTestStatus.PLAYING, viewModel.session?.status)
        assertEquals(0.0, viewModel.elapsedSeconds, 0.0)
    }

    @Test
    fun `tapping the cell numbered 1 advances to the next expected number`() {
        val viewModel = ChimpTestPlayViewModel(FakeSessionStore(), FakeStatsStore(), Difficulty.EASY, resume = false)
        val firstCell = viewModel.session!!.cells.first { it.number == 1 }

        viewModel.onCellTap(firstCell.row, firstCell.col)

        assertEquals(2, viewModel.session?.nextExpected)
    }

    @Test
    fun `tapping a cell outside the grid finalizes the session as a rule-failure loss`() {
        val sessionStore = FakeSessionStore()
        val viewModel = ChimpTestPlayViewModel(sessionStore, FakeStatsStore(), Difficulty.EASY, resume = false)
        val results = mutableListOf<ChimpTestResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.onCellTap(row = -1, col = -1)
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(1200)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals(1, results.size)
        assertEquals(false, results.single().solved)
        assertEquals("rule-failure", results.single().lossReason)
        collectJob.cancel()
    }

    @Test
    fun `endPuzzle finalizes the session as an abandoned loss`() {
        val sessionStore = FakeSessionStore()
        val viewModel = ChimpTestPlayViewModel(sessionStore, FakeStatsStore(), Difficulty.EASY, resume = false)
        val results = mutableListOf<ChimpTestResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endPuzzle()
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals("abandoned", results.single().lossReason)
        collectJob.cancel()
    }
}
