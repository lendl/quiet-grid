package com.quietgrid.engine.arrowescape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowEscapeSolverTest {
    @Test
    fun `a strict two-piece chain solves entirely at tier DIRECT`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(0, 0)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 2)), headDirection = ArrowDirection.LEFT),
        )
        val result = solveArrowEscape(pieces, rows = 1, cols = 3)
        assertTrue(result.solvable)
        assertEquals(ArrowEscapeSolveTier.DIRECT, result.highestTier)
        assertEquals(listOf(0, 1), result.moves.map { it.pieceIndex })
    }

    @Test
    fun `a piece that unblocks exactly one other is tagged SINGLE_BLOCKER over a plain filler`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(0, 2)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 5)), headDirection = ArrowDirection.RIGHT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 4)), headDirection = ArrowDirection.LEFT),
        )
        val result = solveArrowEscape(pieces, rows = 1, cols = 6)
        assertTrue(result.solvable)
        assertEquals(ArrowEscapeSolveTier.SINGLE_BLOCKER, result.highestTier)
        assertEquals(0, result.moves.first().pieceIndex)
        assertEquals(ArrowEscapeSolveTier.SINGLE_BLOCKER, result.moves.first().tier)
    }

    @Test
    fun `a piece starting a two-deep unlock chain is tagged CHAIN_LOOKAHEAD over a plain filler`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(0, 2)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 4)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 6)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 7)), headDirection = ArrowDirection.RIGHT),
        )
        val result = solveArrowEscape(pieces, rows = 1, cols = 8)
        assertTrue(result.solvable)
        assertEquals(ArrowEscapeSolveTier.CHAIN_LOOKAHEAD, result.highestTier)
        assertEquals(0, result.moves.first().pieceIndex)
        assertEquals(ArrowEscapeSolveTier.CHAIN_LOOKAHEAD, result.moves.first().tier)
    }

    @Test
    fun `a piece blocking three others is tagged BOTTLENECK over a plain filler`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(2, 1), CellCoord(2, 2), CellCoord(2, 3)), headDirection = ArrowDirection.RIGHT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 1)), headDirection = ArrowDirection.DOWN),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 2)), headDirection = ArrowDirection.DOWN),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 3)), headDirection = ArrowDirection.DOWN),
            ArrowEscapePiece(cells = listOf(CellCoord(3, 4)), headDirection = ArrowDirection.RIGHT),
        )
        val result = solveArrowEscape(pieces, rows = 4, cols = 5)
        assertTrue(result.solvable)
        assertEquals(ArrowEscapeSolveTier.BOTTLENECK, result.highestTier)
        assertEquals(0, result.moves.first().pieceIndex)
        assertEquals(ArrowEscapeSolveTier.BOTTLENECK, result.moves.first().tier)
    }

    @Test
    fun `two pieces that mutually block each other are unsolvable`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(0, 0)), headDirection = ArrowDirection.RIGHT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 1)), headDirection = ArrowDirection.LEFT),
        )
        val result = solveArrowEscape(pieces, rows = 1, cols = 2)
        assertFalse(result.solvable)
        assertTrue(result.moves.isEmpty())
    }

    @Test
    fun `alreadyRemoved pieces are excluded from the graph and never re-picked`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(0, 0)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 2)), headDirection = ArrowDirection.LEFT),
        )
        val result = solveArrowEscape(pieces, rows = 1, cols = 3, alreadyRemoved = setOf(0))
        assertTrue(result.solvable)
        assertEquals(listOf(1), result.moves.map { it.pieceIndex })
    }

    @Test
    fun `matchesDifficulty checks the highest tier against each difficulty's accepted range`() {
        fun result(tier: ArrowEscapeSolveTier) = ArrowEscapeSolveResult(
            solvable = true,
            highestTier = tier,
            moves = emptyList(),
            longestChainLength = 0,
            bottleneckCount = 0,
            avgBranchingFactor = 0.0,
        )
        assertTrue(result(ArrowEscapeSolveTier.DIRECT).matchesDifficulty(com.quietgrid.engine.core.Difficulty.EASY))
        assertFalse(result(ArrowEscapeSolveTier.CHAIN_LOOKAHEAD).matchesDifficulty(com.quietgrid.engine.core.Difficulty.EASY))
        assertTrue(result(ArrowEscapeSolveTier.BOTTLENECK).matchesDifficulty(com.quietgrid.engine.core.Difficulty.EXPERT))
        assertFalse(result(ArrowEscapeSolveTier.CHAIN_LOOKAHEAD).matchesDifficulty(com.quietgrid.engine.core.Difficulty.EXPERT))
    }

    @Test
    fun `matchesDifficulty is false for an unsolvable result regardless of tier`() {
        val unsolved = ArrowEscapeSolveResult(
            solvable = false,
            highestTier = ArrowEscapeSolveTier.DIRECT,
            moves = emptyList(),
            longestChainLength = 0,
            bottleneckCount = 0,
            avgBranchingFactor = 0.0,
        )
        assertFalse(unsolved.matchesDifficulty(com.quietgrid.engine.core.Difficulty.EASY))
    }

    @Test
    fun `a piece unblocking two others is preferred over one unblocking only one, without reaching bottleneck or chain thresholds`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(2, 1), CellCoord(2, 2)), headDirection = ArrowDirection.RIGHT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 1)), headDirection = ArrowDirection.DOWN),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 2)), headDirection = ArrowDirection.DOWN),
            ArrowEscapePiece(cells = listOf(CellCoord(3, 3)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(3, 5)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 5)), headDirection = ArrowDirection.RIGHT),
        )
        val result = solveArrowEscape(pieces, rows = 4, cols = 6)
        assertTrue(result.solvable)
        assertEquals(0, result.moves.first().pieceIndex)
        assertEquals(ArrowEscapeSolveTier.SINGLE_BLOCKER, result.moves.first().tier)
    }
}
