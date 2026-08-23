package com.quietgrid.app.games.nonogram

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NonogramPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val puzzleEntry = NonogramPuzzleEntry(
        id = "test-nonogram",
        difficulty = Difficulty.EASY.key,
        rows = 1,
        cols = 1,
        solution = listOf(listOf(true)),
    )

    @Before
    fun setUp() {
        mockkObject(NonogramPuzzleBank)
        coEvery { NonogramPuzzleBank.randomPuzzle(any(), any(), any()) } returns puzzleEntry
    }

    @After
    fun tearDown() {
        unmockkObject(NonogramPuzzleBank)
    }

    private fun newViewModel(sessionStore: FakeSessionStore = FakeSessionStore(), statsStore: FakeStatsStore = FakeStatsStore()) =
        NonogramPlayViewModel(mockk<Context>(relaxed = true), sessionStore, statsStore, FakeHistoryStore(), Difficulty.EASY, resume = false)

    @Test
    fun `starting fresh loads the mocked puzzle with an empty board`() {
        val viewModel = newViewModel()

        assertNull(viewModel.session?.board?.get(0)?.get(0))
        assertEquals(listOf(listOf(true)), viewModel.session?.solution)
    }

    @Test
    fun `filling the only cell wins the puzzle`() {
        val sessionStore = FakeSessionStore()
        val viewModel = newViewModel(sessionStore)
        val results = mutableListOf<NonogramResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.onCellTap(0, 0)
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals(1, results.size)
        assertEquals(true, results.single().solved)
        collectJob.cancel()
    }

    @Test
    fun `endPuzzle finalizes the session as an abandoned loss`() {
        val sessionStore = FakeSessionStore()
        val viewModel = newViewModel(sessionStore)
        val results = mutableListOf<NonogramResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endPuzzle()
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals("abandoned", results.single().lossReason)
        collectJob.cancel()
    }
}
