package com.quietgrid.app.games.minesweeper

import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MinesweeperPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starting fresh creates an ungenerated playable session with zero elapsed seconds`() {
        val viewModel = MinesweeperPlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)

        assertNotNull(viewModel.session)
        assertEquals(MinesweeperStatus.PLAYING, viewModel.session?.board?.status)
        assertFalse(viewModel.session!!.board.generated)
        assertEquals(0.0, viewModel.elapsedSeconds, 0.0)
    }

    @Test
    fun `flagging before the board is generated still flags the cell`() {
        val viewModel = MinesweeperPlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)

        viewModel.onToggleFlag(0, 0)

        assertEquals(MinesweeperCellState.FLAGGED, viewModel.session!!.board.cells[0][0].state)
    }

    @Test
    fun `revealing the first cell is always safe and generates the board`() {
        val viewModel = MinesweeperPlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)

        viewModel.onReveal(0, 0)

        val board = viewModel.session!!.board
        assertTrue(board.generated)
        assertEquals(MinesweeperStatus.PLAYING, board.status)
        assertEquals(MinesweeperCellState.REVEALED, board.cells[0][0].state)
    }

    @Test
    fun `revealing an out-of-bounds cell leaves the session unchanged`() {
        val viewModel = MinesweeperPlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)
        val before = viewModel.session

        viewModel.onReveal(-1, -1)

        assertEquals(before, viewModel.session)
    }

    @Test
    fun `endPuzzle finalizes the session as an abandoned loss`() {
        val sessionStore = FakeSessionStore()
        val viewModel = MinesweeperPlayViewModel(sessionStore, FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)
        val results = mutableListOf<MinesweeperResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endPuzzle()
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals("abandoned", results.single().lossReason)
        collectJob.cancel()
    }
}
