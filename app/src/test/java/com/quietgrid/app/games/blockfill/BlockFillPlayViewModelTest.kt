package com.quietgrid.app.games.blockfill

import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BlockFillPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starting fresh creates a playable session with zero elapsed seconds`() {
        val viewModel = BlockFillPlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)

        assertNotNull(viewModel.session)
        assertEquals(BlockFillStatus.PLAYING, viewModel.session?.status)
        assertEquals(0.0, viewModel.elapsedSeconds, 0.0)
    }

    @Test
    fun `placing a piece at an out-of-range tray index leaves the session unchanged`() {
        val viewModel = BlockFillPlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)
        val before = viewModel.session

        viewModel.onPlacePiece(pieceIndex = 99, anchorRow = 0, anchorCol = 0)

        assertEquals(before, viewModel.session)
    }

    @Test
    fun `placing a piece at an out-of-range tray index returns false`() {
        val viewModel = BlockFillPlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)

        val accepted = viewModel.onPlacePiece(pieceIndex = 99, anchorRow = 0, anchorCol = 0)

        assertEquals(false, accepted)
    }

    @Test
    fun `placing a piece at a valid placement returns true`() {
        val viewModel = BlockFillPlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)
        val session = viewModel.session!!
        val (pieceIndex, anchor) = session.tray.withIndex().firstNotNullOf { (index, piece) ->
            piece?.let { p -> findValidPlacements(session.board, p.cells).firstOrNull()?.let { index to it } }
        }

        val accepted = viewModel.onPlacePiece(pieceIndex, anchor.first, anchor.second)

        assertEquals(true, accepted)
    }

    @Test
    fun `endPuzzle clears the session store and emits an abandoned loss`() {
        val sessionStore = FakeSessionStore()
        val viewModel = BlockFillPlayViewModel(sessionStore, FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)
        val results = mutableListOf<BlockFillResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endPuzzle()
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals(1, results.size)
        assertEquals(false, results.single().solved)
        assertEquals("abandoned", results.single().lossReason)
        collectJob.cancel()
    }
}
