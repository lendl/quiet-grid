package com.quietgrid.engine.arrowescape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowEscapeDifficultyScoreTest {
    @Test
    fun `a hub blocking three same-direction pieces contributes zero score`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(2, 1), CellCoord(2, 2), CellCoord(2, 3)), headDirection = ArrowDirection.RIGHT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 1)), headDirection = ArrowDirection.DOWN),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 2)), headDirection = ArrowDirection.DOWN),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 3)), headDirection = ArrowDirection.DOWN),
        )
        assertEquals(0.0, computeBottleneckScore(pieces, rows = 4, cols = 5), 0.0001)
    }

    @Test
    fun `a hub blocking pieces across three distinct directions scores higher than across two`() {
        val hub = ArrowEscapePiece(cells = listOf(CellCoord(2, 1), CellCoord(2, 2), CellCoord(2, 3)), headDirection = ArrowDirection.UP)
        val down = ArrowEscapePiece(cells = listOf(CellCoord(0, 1)), headDirection = ArrowDirection.DOWN)
        val up = ArrowEscapePiece(cells = listOf(CellCoord(4, 2)), headDirection = ArrowDirection.UP)
        val left = ArrowEscapePiece(cells = listOf(CellCoord(2, 5)), headDirection = ArrowDirection.LEFT)

        val twoDirections = computeBottleneckScore(listOf(hub, down, left), rows = 5, cols = 6)
        val threeDirections = computeBottleneckScore(listOf(hub, down, up, left), rows = 5, cols = 6)

        assertEquals(2.0, twoDirections, 0.0001)
        assertEquals(6.0, threeDirections, 0.0001)
        assertTrue(threeDirections > twoDirections)
    }

    @Test
    fun `a strict cascading chain produces the same needle score every run since no choice ever exists`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(0, 0)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 2)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 4)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 6)), headDirection = ArrowDirection.LEFT),
        )
        val score = computeNeedleScore(pieces, rows = 1, cols = 7)
        assertEquals(10.0, score, 0.0001)
    }

    @Test
    fun `scoreArrowEscapePuzzle combines both signals and normalizes by piece count`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(0, 0)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 2)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 4)), headDirection = ArrowDirection.LEFT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 6)), headDirection = ArrowDirection.LEFT),
        )
        val score = scoreArrowEscapePuzzle(pieces, rows = 1, cols = 7)
        assertEquals(0.0, score.bottleneckScore, 0.0001)
        assertEquals(10.0, score.needleScore, 0.0001)
        assertEquals(5.0, score.normalizedScore, 0.0001)
    }

    @Test
    fun `matchesDifficulty buckets normalizedScore into the correct half-open range`() {
        val boundaries = ArrowEscapeScoreBoundaries(mediumFloor = 10.0, hardFloor = 20.0, expertFloor = 30.0)
        fun result(score: Double) = ArrowEscapeDifficultyScore(bottleneckScore = 0.0, needleScore = 0.0, normalizedScore = score)

        assertTrue(result(5.0).matchesDifficulty(com.quietgrid.engine.core.Difficulty.EASY, boundaries))
        assertFalse(result(10.0).matchesDifficulty(com.quietgrid.engine.core.Difficulty.EASY, boundaries))
        assertTrue(result(10.0).matchesDifficulty(com.quietgrid.engine.core.Difficulty.MEDIUM, boundaries))
        assertTrue(result(19.999).matchesDifficulty(com.quietgrid.engine.core.Difficulty.MEDIUM, boundaries))
        assertTrue(result(20.0).matchesDifficulty(com.quietgrid.engine.core.Difficulty.HARD, boundaries))
        assertTrue(result(29.999).matchesDifficulty(com.quietgrid.engine.core.Difficulty.HARD, boundaries))
        assertTrue(result(30.0).matchesDifficulty(com.quietgrid.engine.core.Difficulty.EXPERT, boundaries))
    }
}
