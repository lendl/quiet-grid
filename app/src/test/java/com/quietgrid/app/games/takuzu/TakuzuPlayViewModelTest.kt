package com.quietgrid.app.games.takuzu

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry
import com.quietgrid.engine.takuzu.gridToHex
import com.quietgrid.engine.takuzu.maskToHex
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

class TakuzuPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val solutionGrid = listOf(
        listOf(0, 1, 0, 1),
        listOf(1, 0, 1, 0),
        listOf(0, 1, 1, 0),
        listOf(1, 0, 0, 1),
    )
    private val maskGrid = listOf(
        listOf(false, true, true, true),
        listOf(true, true, true, true),
        listOf(true, true, true, true),
        listOf(true, true, true, true),
    )
    private val puzzleEntry = TakuzuPuzzleEntry(
        id = "test-takuzu",
        size = 4,
        difficulty = Difficulty.EASY.key,
        solution = gridToHex(solutionGrid),
        mask = maskToHex(maskGrid),
    )

    @Before
    fun setUp() {
        mockkObject(TakuzuPuzzleBank)
        coEvery { TakuzuPuzzleBank.randomPuzzle(any(), any()) } returns puzzleEntry
    }

    @After
    fun tearDown() {
        unmockkObject(TakuzuPuzzleBank)
    }

    private fun newViewModel(sessionStore: FakeSessionStore = FakeSessionStore(), statsStore: FakeStatsStore = FakeStatsStore()) =
        TakuzuPlayViewModel(mockk<Context>(relaxed = true), sessionStore, statsStore, Difficulty.EASY, resume = false)

    @Test
    fun `starting fresh loads the mocked puzzle with only the ungiven cell blank`() {
        val viewModel = newViewModel()

        assertNull(viewModel.session?.board?.get(0)?.get(0))
        assertEquals(1, viewModel.session?.board?.get(0)?.get(1))
    }

    @Test
    fun `pressing the last ungiven cell to its solution value wins the puzzle`() {
        val sessionStore = FakeSessionStore()
        val viewModel = newViewModel(sessionStore)
        val results = mutableListOf<TakuzuResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.onCellPress(0, 0)
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(1300)
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
        val results = mutableListOf<TakuzuResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endPuzzle()
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals("abandoned", results.single().lossReason)
        collectJob.cancel()
    }
}
