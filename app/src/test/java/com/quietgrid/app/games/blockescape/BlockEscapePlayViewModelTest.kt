package com.quietgrid.app.games.blockescape

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.blockescape.BlockEscapeBlock
import com.quietgrid.engine.blockescape.BlockEscapeMove
import com.quietgrid.engine.blockescape.BlockEscapePuzzleEntry
import com.quietgrid.engine.blockescape.findBlockEscapeHint
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val sampleBoard = listOf(
    BlockEscapeBlock(2, 0, 2, true),
    BlockEscapeBlock(1, 2, 2, false),
    BlockEscapeBlock(2, 4, 3, false),
    BlockEscapeBlock(5, 3, 2, true),
    BlockEscapeBlock(0, 1, 3, true),
    BlockEscapeBlock(3, 2, 2, false),
    BlockEscapeBlock(4, 0, 2, true),
)

private val samplePuzzle = BlockEscapePuzzleEntry(id = "five", difficulty = "easy", blocks = sampleBoard, minMoves = 5)

@OptIn(ExperimentalCoroutinesApi::class)
class BlockEscapePlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkObject(BlockEscapePuzzleBank)
        coEvery { BlockEscapePuzzleBank.randomPuzzle(any(), any(), any()) } returns samplePuzzle
    }

    @After
    fun tearDown() {
        unmockkObject(BlockEscapePuzzleBank)
    }

    private fun newViewModel(): BlockEscapePlayViewModel {
        val viewModel = BlockEscapePlayViewModel(
            mockk<Context>(relaxed = true),
            FakeSessionStore(),
            FakeStatsStore(),
            FakeHistoryStore(),
            Difficulty.EASY,
            resume = false,
        )
        viewModel.hintDispatcher = StandardTestDispatcher(mainDispatcherRule.dispatcher.scheduler)
        return viewModel
    }

    private fun runHint() = mainDispatcherRule.dispatcher.scheduler.runCurrent()

    @Test
    fun `a hint highlights the first move of a shortest solution without moving`() {
        val vm = newViewModel()
        vm.onHint()
        runHint()
        assertEquals(findBlockEscapeHint(sampleBoard), vm.hint)
        assertEquals(sampleBoard, vm.session?.blocks)
        assertEquals(0, vm.session?.moves)
        assertEquals(1, vm.session?.hintsUsed)
    }

    @Test
    fun `pressing hint again turns it off, and showing it again on the same board is free`() {
        val vm = newViewModel()
        vm.onHint()
        runHint()
        vm.onHint()
        assertNull(vm.hint)
        assertEquals(1, vm.session?.hintsUsed)
        vm.onHint()
        runHint()
        assertNotNull(vm.hint)
        assertEquals(1, vm.session?.hintsUsed)
    }

    @Test
    fun `a move clears the hint`() {
        val vm = newViewModel()
        vm.onHint()
        runHint()
        vm.onMove(BlockEscapeMove(3, 2))
        assertNull(vm.hint)
    }

    @Test
    fun `dragging while a hint is computing cancels the hint and applies the drag`() {
        val vm = newViewModel()
        vm.onHint()
        vm.onMove(BlockEscapeMove(3, 2))
        assertEquals(1, vm.session?.moves)
        assertFalse(vm.isComputingHint)
        runHint()
        assertNull(vm.hint)
        assertEquals(0, vm.session?.hintsUsed)
    }

    @Test
    fun `undo keeps the hint cost and clears the highlight`() {
        val vm = newViewModel()
        vm.onMove(BlockEscapeMove(3, 2))
        vm.onHint()
        runHint()
        vm.onUndo()
        assertNull(vm.hint)
        assertEquals(1, vm.session?.hintsUsed)
        assertEquals(0, vm.session?.moves)
    }
}
