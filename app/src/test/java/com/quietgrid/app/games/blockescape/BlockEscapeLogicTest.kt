package com.quietgrid.app.games.blockescape

import com.quietgrid.engine.blockescape.BlockEscapeBlock
import com.quietgrid.engine.blockescape.BlockEscapeMove
import com.quietgrid.engine.blockescape.BlockEscapePuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockEscapeLogicTest {
    private val puzzle = BlockEscapePuzzleEntry(
        id = "be-test",
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

    private val solution = listOf(
        BlockEscapeMove(3, 2),
        BlockEscapeMove(2, 3),
        BlockEscapeMove(4, 3),
        BlockEscapeMove(1, 0),
        BlockEscapeMove(0, 4),
    )

    @Test
    fun `playing the shortest solution wins with full accuracy`() {
        val won = solution.fold(createBlockEscapeSession(puzzle)) { session, move -> applyBlockEscapeSlide(session, move)!! }
        assertEquals(BlockEscapeStatus.WON, won.status)
        assertEquals(5, won.moves)
        assertEquals(100, blockEscapeAccuracy(won))
        assertEquals(1000, blockEscapeScore(won, 0))
    }

    @Test
    fun `a blocked slide is refused and costs nothing`() {
        val session = createBlockEscapeSession(puzzle)
        assertNull(applyBlockEscapeSlide(session, BlockEscapeMove(0, 3)))
    }

    @Test
    fun `extra moves, hints and time lower the score`() {
        val session = createBlockEscapeSession(puzzle).copy(moves = 9, hintsUsed = 1)
        assertEquals(1000 - 100 - 40 - 30, blockEscapeScore(session, 30))
        assertEquals(55, blockEscapeAccuracy(session))
    }

    @Test
    fun `charging a hint adds one hint and leaves the board and moves alone`() {
        val moved = applyBlockEscapeSlide(createBlockEscapeSession(puzzle), BlockEscapeMove(3, 2))!!
        val charged = chargeBlockEscapeHint(moved)
        assertNotNull(charged)
        assertEquals(1, charged!!.hintsUsed)
        assertEquals(1, charged.moves)
        assertEquals(moved.blocks, charged.blocks)
        assertNull(chargeBlockEscapeHint(moved.copy(status = BlockEscapeStatus.WON)))
    }

    @Test
    fun `restored sessions must keep every block in its own lane`() {
        val session = createBlockEscapeSession(puzzle)
        assertTrue(isRestorableBlockEscapeSession(session))
        val shifted = session.copy(blocks = session.blocks.mapIndexed { i, b -> if (i == 3) b.copy(row = 4) else b })
        assertFalse(isRestorableBlockEscapeSession(shifted))
    }
}
