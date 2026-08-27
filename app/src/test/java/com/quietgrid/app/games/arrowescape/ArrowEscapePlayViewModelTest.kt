package com.quietgrid.app.games.arrowescape

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.arrowescape.ArrowEscapePieceData
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private fun twoPiecePuzzle() = ArrowEscapePuzzleEntry(
    id = "test-arrowescape",
    difficulty = Difficulty.EASY.key,
    rows = 2,
    cols = 1,
    pieces = listOf(
        ArrowEscapePieceData(cells = listOf(listOf(0, 0)), headDirection = "down"),
        ArrowEscapePieceData(cells = listOf(listOf(1, 0)), headDirection = "down"),
    ),
)

class ArrowEscapePlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val puzzleEntry = twoPiecePuzzle()

    @Before
    fun setUp() {
        mockkObject(ArrowEscapePuzzleBank)
        coEvery { ArrowEscapePuzzleBank.randomPuzzle(any(), any(), any()) } returns puzzleEntry
    }

    @After
    fun tearDown() {
        unmockkObject(ArrowEscapePuzzleBank)
    }

    private fun newViewModel(sessionStore: FakeSessionStore = FakeSessionStore(), statsStore: FakeStatsStore = FakeStatsStore()) =
        ArrowEscapePlayViewModel(mockk<Context>(relaxed = true), sessionStore, statsStore, FakeHistoryStore(), Difficulty.EASY, resume = false)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `requesting a hint computes asynchronously and ignores re-entrant taps while pending`() {
        val viewModel = newViewModel()
        viewModel.hintDispatcher = StandardTestDispatcher(mainDispatcherRule.dispatcher.scheduler)

        viewModel.onHint()
        assertTrue(viewModel.isComputingHint)
        assertNull(viewModel.session?.selectedIndex)

        viewModel.onHint()
        assertTrue(viewModel.isComputingHint)

        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.isComputingHint)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `tapping a piece while the hint is computing cancels it instead of applying a stale result`() {
        val viewModel = newViewModel()
        viewModel.hintDispatcher = StandardTestDispatcher(mainDispatcherRule.dispatcher.scheduler)

        viewModel.onHint()
        assertTrue(viewModel.isComputingHint)

        viewModel.onPieceTap(1)
        assertFalse(viewModel.isComputingHint)
        val selectedAfterTap = viewModel.session?.selectedIndex

        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.isComputingHint)
        assertEquals(selectedAfterTap, viewModel.session?.selectedIndex)
    }
}
