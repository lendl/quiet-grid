package com.quietgrid.app.games.game2048

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

class Game2048PlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starting fresh creates a playable session sized for the difficulty`() {
        val viewModel = Game2048PlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)

        assertNotNull(viewModel.session)
        assertEquals(5, viewModel.session?.puzzle?.size)
        assertEquals(Game2048Status.PLAYING, viewModel.session?.board?.status)
        assertEquals(0.0, viewModel.elapsedSeconds, 0.0)
    }

    @Test
    fun `a swipe that changes the board updates the session`() {
        val viewModel = Game2048PlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.MEDIUM, resume = false)
        val before = viewModel.session!!.board

        Game2048Direction.entries.forEach { viewModel.onSwipe(it) }

        assertTrue(viewModel.session!!.board.moveCount > before.moveCount)
    }

    @Test
    fun `endPuzzle finalizes the session as an abandoned loss`() {
        val sessionStore = FakeSessionStore()
        val viewModel = Game2048PlayViewModel(sessionStore, FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)
        val results = mutableListOf<Game2048Result>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endPuzzle()
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals("abandoned", results.single().lossReason)
        collectJob.cancel()
    }
}
