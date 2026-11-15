package com.quietgrid.engine.blockescape

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BlockEscapeSolverTest {
    private val fiveMoveBoard = listOf(
        BlockEscapeBlock(2, 0, 2, true),
        BlockEscapeBlock(1, 2, 2, false),
        BlockEscapeBlock(2, 4, 3, false),
        BlockEscapeBlock(5, 3, 2, true),
        BlockEscapeBlock(0, 1, 3, true),
        BlockEscapeBlock(3, 2, 2, false),
        BlockEscapeBlock(4, 0, 2, true),
    )

    private val backAndForthBoard = listOf(
        BlockEscapeBlock(2, 0, 2, true),
        BlockEscapeBlock(1, 2, 2, true),
        BlockEscapeBlock(4, 4, 2, true),
        BlockEscapeBlock(3, 1, 2, true),
        BlockEscapeBlock(2, 5, 2, false),
        BlockEscapeBlock(5, 4, 2, true),
        BlockEscapeBlock(0, 5, 2, false),
        BlockEscapeBlock(3, 3, 3, false),
    )

    @Test
    fun `the shortest solution counts each slide as one move`() {
        val solution = solveBlockEscape(fiveMoveBoard)
        assertNotNull(solution)
        assertEquals(5, solution!!.size)
        val finished = solution.fold(fiveMoveBoard) { blocks, move -> applyBlockEscapeMove(blocks, move)!! }
        assertTrue(isBlockEscapeSolved(finished))
    }

    @Test
    fun `an already solved board needs no moves`() {
        assertEquals(0, blockEscapeMinMoves(listOf(BlockEscapeBlock(2, 4, 2, true))))
    }

    @Test
    fun `a board with a wall in front of the key block has no solution`() {
        val blocked = listOf(
            BlockEscapeBlock(2, 0, 2, true),
            BlockEscapeBlock(0, 3, 3, false),
            BlockEscapeBlock(3, 3, 3, false),
        )
        assertNull(solveBlockEscape(blocked))
    }

    @Test
    fun `the hint is the first move of a shortest solution`() {
        val hint = findBlockEscapeHint(fiveMoveBoard)!!
        val after = applyBlockEscapeMove(fiveMoveBoard, hint)!!
        assertEquals(4, blockEscapeMinMoves(after))
    }

    @Test
    fun `distances agree with the solver`() {
        val distances = blockEscapeDistances(fiveMoveBoard)!!
        assertTrue(distances.farthest >= 5)
        distances.statesAt(3..3).take(5).forEach { key ->
            assertEquals(3, blockEscapeMinMoves(distances.blocksOf(key)))
        }
    }

    @Test
    fun `grading follows the move and repeat ranges`() {
        assertEquals(Difficulty.EASY, gradeBlockEscape(5) { 0 })
        assertEquals(Difficulty.EASY, gradeBlockEscape(8) { 0 })
        assertEquals(Difficulty.MEDIUM, gradeBlockEscape(8) { 1 })
        assertEquals(Difficulty.MEDIUM, gradeBlockEscape(12) { 4 })
        assertEquals(Difficulty.HARD, gradeBlockEscape(20) { null })
        assertEquals(Difficulty.EXPERT, gradeBlockEscape(40) { null })
        assertNull(gradeBlockEscape(6) { 1 })
        assertNull(gradeBlockEscape(12) { 5 })
        assertNull(gradeBlockEscape(15) { 0 })
    }

    @Test
    fun `hard and expert grading never counts repeat moves`() {
        assertEquals(Difficulty.HARD, gradeBlockEscape(20) { error("not needed") })
        assertEquals(Difficulty.EXPERT, gradeBlockEscape(40) { error("not needed") })
    }

    @Test
    fun `a board where each block slides once has no repeat moves`() {
        assertEquals(0, blockEscapeRepeatMoves(fiveMoveBoard))
    }

    @Test
    fun `a board that needs a block to slide twice counts the repeat`() {
        assertEquals(6, blockEscapeMinMoves(backAndForthBoard))
        assertEquals(1, blockEscapeRepeatMoves(backAndForthBoard))
        assertNull(gradeBlockEscape(backAndForthBoard))
    }

    @Test
    fun `generated puzzles land in their tier`() {
        val random = Random(7)
        listOf(Difficulty.EASY, Difficulty.MEDIUM).forEach { difficulty ->
            val (blocks, minMoves) = generateBlockEscapePuzzle(difficulty, random)!!
            assertTrue(isValidBlockEscapeLayout(blocks))
            assertEquals(minMoves, blockEscapeMinMoves(blocks))
            assertEquals(difficulty, gradeBlockEscape(blocks))
        }
    }
}
