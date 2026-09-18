package com.quietgrid.app.games.nback

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

class NBackPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starting fresh creates a 32-trial session and begins the stimulus loop`() {
        val viewModel = NBackPlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(NBACK_START_DELAY_MS)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertNotNull(viewModel.session)
        assertEquals(NBACK_TOTAL_TRIALS, viewModel.session?.trials?.size)
        assertEquals(0, viewModel.session?.currentIndex)
    }

    @Test
    fun `tapping match marks the current trial responded with a reaction time`() {
        val viewModel = NBackPlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(NBACK_START_DELAY_MS)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        val currentIndex = viewModel.session!!.currentIndex

        viewModel.onMatchTap()

        val trial = viewModel.session!!.trials[currentIndex]
        assertTrue(trial.responded)
        assertNotNull(trial.reactionTimeMs)
    }

    @Test
    fun `tapping match twice on the same trial does not overwrite the first response`() {
        val viewModel = NBackPlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(NBACK_START_DELAY_MS)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        val currentIndex = viewModel.session!!.currentIndex

        viewModel.onMatchTap()
        val firstReactionTime = viewModel.session!!.trials[currentIndex].reactionTimeMs
        viewModel.onMatchTap()

        assertEquals(firstReactionTime, viewModel.session!!.trials[currentIndex].reactionTimeMs)
    }

    @Test
    fun `the stimulus loop advances to later trials as time passes`() {
        val viewModel = NBackPlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(NBACK_START_DELAY_MS + 2500 * 3 + 100)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.session!!.currentIndex >= 3)
    }

    @Test
    fun `completing every trial finishes the round as a win`() {
        val viewModel = NBackPlayViewModel(FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)
        val results = mutableListOf<NBackResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(NBACK_START_DELAY_MS + 2500L * (NBACK_TOTAL_TRIALS + 1))
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(results.single().solved)
        collectJob.cancel()
    }

    @Test
    fun `endPuzzle finalizes the session as an abandoned loss`() {
        val sessionStore = FakeSessionStore()
        val viewModel = NBackPlayViewModel(sessionStore, FakeStatsStore(), FakeHistoryStore(), Difficulty.EASY, resume = false)
        val results = mutableListOf<NBackResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endPuzzle()
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals("abandoned", results.single().lossReason)
        collectJob.cancel()
    }
}
