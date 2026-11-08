package com.quietgrid.app.games.blockescape

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.session.AppForeground
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.blockescape.BlockEscapeBlock
import com.quietgrid.engine.blockescape.BlockEscapeMove
import com.quietgrid.engine.blockescape.BlockEscapePuzzleEntry
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val oneMove = BlockEscapePuzzleEntry(
    id = "one",
    difficulty = "easy",
    blocks = listOf(BlockEscapeBlock(2, 0, 2, true)),
    minMoves = 1,
)

private val fiveMove = BlockEscapePuzzleEntry(
    id = "five",
    difficulty = "easy",
    blocks = listOf(
        BlockEscapeBlock(2, 0, 2, true),
        BlockEscapeBlock(1, 2, 2, false),
        BlockEscapeBlock(2, 4, 3, false),
        BlockEscapeBlock(5, 3, 2, true),
        BlockEscapeBlock(0, 1, 3, true),
        BlockEscapeBlock(3, 2, 2, false),
        BlockEscapeBlock(4, 0, 2, true),
    ),
    minMoves = 5,
)

class BlockEscapeChallengerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @After
    fun tearDown() {
        unmockkObject(BlockEscapePuzzleBank)
    }

    private fun newViewModel(): BlockEscapeChallengerViewModel {
        mockkObject(BlockEscapePuzzleBank)
        coEvery { BlockEscapePuzzleBank.randomPuzzle(any(), any(), any()) } returnsMany listOf(oneMove, fiveMove, fiveMove)
        return BlockEscapeChallengerViewModel(mockk<Context>(relaxed = true), FakeStatsStore(), FakeHistoryStore(), AppForeground { true })
    }

    @Test
    fun `undo starts disabled and does nothing with an empty history`() {
        val vm = newViewModel()
        assertFalse(vm.canUndo)
        val before = vm.session
        vm.onUndo()
        assertEquals(before, vm.session)
    }

    @Test
    fun `undo after a solve cannot bring back the previous puzzle`() {
        val vm = newViewModel()
        vm.onMove(BlockEscapeMove(0, 4))
        assertEquals("five", vm.session?.puzzleSession?.puzzle?.id)
        assertFalse(vm.canUndo)
        vm.onUndo()
        assertEquals("five", vm.session?.puzzleSession?.puzzle?.id)
    }

    @Test
    fun `undo takes back one slide and the move count`() {
        val vm = newViewModel()
        vm.onMove(BlockEscapeMove(0, 4))
        vm.onMove(BlockEscapeMove(3, 2))
        assertTrue(vm.canUndo)
        vm.onUndo()
        assertEquals(0, vm.session?.puzzleSession?.moves)
        assertEquals(vm.session?.puzzleSession?.puzzle?.blocks, vm.session?.puzzleSession?.blocks)
    }
}
