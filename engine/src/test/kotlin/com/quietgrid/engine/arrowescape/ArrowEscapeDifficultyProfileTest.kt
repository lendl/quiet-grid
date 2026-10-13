package com.quietgrid.engine.arrowescape

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Test

class ArrowEscapeDifficultyProfileTest {
    private fun piece(row: Int, col: Int, direction: ArrowDirection) = ArrowEscapePiece(listOf(CellCoord(row, col)), direction)

    @Test
    fun `a single-row cascade has exactly one free arrow at every step`() {
        val pieces = listOf(0, 2, 4, 6).map { piece(0, it, ArrowDirection.LEFT) }
        val profile = profileArrowEscapePuzzle(pieces, rows = 1, cols = 7)
        assertEquals(4, profile.pieceCount)
        assertEquals(4, profile.rounds)
        assertEquals((1.0 / 4 + 1.0 / 3 + 1.0 / 2 + 1.0) / 4, profile.freeShare, 1e-9)
        assertEquals(0.25, profile.borderExitShare, 1e-9)
        assertEquals(2.0, profile.meanBlockers, 1e-9)
    }

    @Test
    fun `independent border exits are all free in one round`() {
        val pieces = listOf(piece(0, 1, ArrowDirection.UP), piece(1, 0, ArrowDirection.LEFT), piece(2, 1, ArrowDirection.DOWN))
        val profile = profileArrowEscapePuzzle(pieces, rows = 3, cols = 3)
        assertEquals(1, profile.rounds)
        assertEquals(1.0, profile.freeShare, 1e-9)
        assertEquals(1.0, profile.borderExitShare, 1e-9)
        assertEquals(0.0, profile.meanBlockers, 1e-9)
    }

    @Test
    fun `a deadlocked pair stops without looping`() {
        val pieces = listOf(piece(0, 0, ArrowDirection.RIGHT), piece(0, 1, ArrowDirection.LEFT))
        val profile = profileArrowEscapePuzzle(pieces, rows = 1, cols = 2)
        assertEquals(0, profile.rounds)
        assertEquals(0.0, profile.freeShare, 1e-9)
    }

    @Test
    fun `profiling the same puzzle twice gives an equal profile`() {
        val pieces = listOf(
            piece(0, 0, ArrowDirection.LEFT),
            piece(0, 2, ArrowDirection.LEFT),
            piece(1, 0, ArrowDirection.LEFT),
            piece(1, 2, ArrowDirection.LEFT),
            piece(1, 4, ArrowDirection.LEFT),
        )
        assertEquals(profileArrowEscapePuzzle(pieces, 2, 5), profileArrowEscapePuzzle(pieces, 2, 5))
    }

    @Test
    fun `classify buckets free share at the floors`() {
        fun at(share: Double) = classifyArrowEscapeDifficulty(ArrowEscapeDifficultyProfile(10, 3, share, 0.1, 2.0))
        assertEquals(Difficulty.EASY, at(0.52))
        assertEquals(Difficulty.MEDIUM, at(0.5199))
        assertEquals(Difficulty.MEDIUM, at(0.40))
        assertEquals(Difficulty.HARD, at(0.3999))
        assertEquals(Difficulty.HARD, at(0.29))
        assertEquals(Difficulty.EXPERT, at(0.2899))
    }
}
