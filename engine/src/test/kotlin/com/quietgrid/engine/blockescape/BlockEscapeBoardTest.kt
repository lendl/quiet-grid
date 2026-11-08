package com.quietgrid.engine.blockescape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockEscapeBoardTest {
    private val board = listOf(
        BlockEscapeBlock(2, 0, 2, true),
        BlockEscapeBlock(1, 2, 2, false),
        BlockEscapeBlock(2, 4, 3, false),
        BlockEscapeBlock(5, 3, 2, true),
        BlockEscapeBlock(0, 1, 3, true),
        BlockEscapeBlock(3, 2, 2, false),
        BlockEscapeBlock(4, 0, 2, true),
    )

    @Test
    fun `the sample board is a valid layout`() {
        assertTrue(isValidBlockEscapeLayout(board))
    }

    @Test
    fun `overlapping or out of bounds blocks are rejected`() {
        assertFalse(isValidBlockEscapeLayout(board + BlockEscapeBlock(2, 1, 2, false)))
        assertFalse(isValidBlockEscapeLayout(board + BlockEscapeBlock(0, 5, 2, true)))
        assertFalse(isValidBlockEscapeLayout(listOf(BlockEscapeBlock(3, 0, 2, true))))
    }

    @Test
    fun `a block slides only along its own lane until something is in the way`() {
        assertEquals(1..1, blockEscapeSlideRange(board, 1))
        assertEquals(0..4, blockEscapeSlideRange(board, 3))
        assertEquals(0..2, blockEscapeSlideRange(board, 2))
        assertEquals(0..0, blockEscapeSlideRange(board, BLOCKESCAPE_KEY_BLOCK))
    }

    @Test
    fun `moves outside the slide range or to the same spot are refused`() {
        assertNull(applyBlockEscapeMove(board, BlockEscapeMove(BLOCKESCAPE_KEY_BLOCK, 3)))
        assertNull(applyBlockEscapeMove(board, BlockEscapeMove(3, 3)))
        val moved = applyBlockEscapeMove(board, BlockEscapeMove(3, 2))
        assertEquals(BlockEscapeBlock(5, 2, 2, true), moved?.get(3))
    }

    @Test
    fun `the puzzle is solved once the key block reaches the exit`() {
        assertFalse(isBlockEscapeSolved(board))
        assertTrue(isBlockEscapeSolved(listOf(BlockEscapeBlock(2, 4, 2, true))))
    }

    @Test
    fun `block lookup finds the block covering a cell`() {
        assertEquals(2, blockEscapeBlockAt(board, 4, 4))
        assertNull(blockEscapeBlockAt(board, 1, 0))
    }

    @Test
    fun `layout key ignores the order of non-key blocks`() {
        val shuffled = listOf(board[0]) + board.drop(1).reversed()
        assertEquals(blockEscapeLayoutKey(board), blockEscapeLayoutKey(shuffled))
    }

    @Test
    fun `lanes match only when every block stays in its own lane`() {
        assertTrue(blockEscapeLanesMatch(board, board))
        assertTrue(blockEscapeLanesMatch(board, applyBlockEscapeMove(board, BlockEscapeMove(3, 2))!!))
        assertFalse(blockEscapeLanesMatch(board, board.mapIndexed { i, b -> if (i == 3) b.copy(row = 4) else b }))
        assertFalse(blockEscapeLanesMatch(board, board.dropLast(1)))
    }
}
