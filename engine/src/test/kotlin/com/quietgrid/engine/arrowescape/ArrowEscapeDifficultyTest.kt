// engine/src/test/kotlin/com/quietgrid/engine/arrowescape/ArrowEscapeDifficultyTest.kt
package com.quietgrid.engine.arrowescape

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowEscapeDifficultyTest {
    @Test
    fun `classifyArrowEscapeDifficulty is gapless - chokepoint count alone decides the tier`() {
        assertEquals(Difficulty.EASY, classifyArrowEscapeDifficulty(ArrowEscapeDifficultyMetrics(maxFanOut = 2, chokepointCount = 0)))
        assertEquals(Difficulty.MEDIUM, classifyArrowEscapeDifficulty(ArrowEscapeDifficultyMetrics(maxFanOut = 99, chokepointCount = 1)))
        assertEquals(Difficulty.MEDIUM, classifyArrowEscapeDifficulty(ArrowEscapeDifficultyMetrics(maxFanOut = 0, chokepointCount = 2)))
        assertEquals(Difficulty.HARD, classifyArrowEscapeDifficulty(ArrowEscapeDifficultyMetrics(maxFanOut = 5, chokepointCount = 3)))
        assertEquals(Difficulty.HARD, classifyArrowEscapeDifficulty(ArrowEscapeDifficultyMetrics(maxFanOut = 5, chokepointCount = 5)))
        assertEquals(Difficulty.EXPERT, classifyArrowEscapeDifficulty(ArrowEscapeDifficultyMetrics(maxFanOut = 3, chokepointCount = 6)))
        assertEquals(Difficulty.HARD, classifyArrowEscapeDifficulty(ArrowEscapeDifficultyMetrics(maxFanOut = 2, chokepointCount = 3)))
    }

    @Test
    fun `chainLengthForDifficulty returns strictly increasing lengths across tiers`() {
        val easy = chainLengthForDifficulty(Difficulty.EASY)
        val medium = chainLengthForDifficulty(Difficulty.MEDIUM)
        val hard = chainLengthForDifficulty(Difficulty.HARD)
        val expert = chainLengthForDifficulty(Difficulty.EXPERT)
        assertTrue(easy < medium)
        assertTrue(medium < hard)
        assertTrue(hard < expert)
    }

    @Test
    fun `computeArrowEscapeScore increases with more severe metrics`() {
        val low = computeArrowEscapeScore(ArrowEscapeDifficultyMetrics(maxFanOut = 1, chokepointCount = 0))
        val high = computeArrowEscapeScore(ArrowEscapeDifficultyMetrics(maxFanOut = 12, chokepointCount = 9))
        assertTrue(high > low)
    }

    @Test
    fun `measureArrowEscapePuzzle reports max fan-out and chokepoint count from the graph`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(0, 0)), headDirection = ArrowDirection.RIGHT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 1)), headDirection = ArrowDirection.RIGHT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 2)), headDirection = ArrowDirection.RIGHT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 3)), headDirection = ArrowDirection.RIGHT),
        )
        val graph = buildDependencyGraph(pieces, rows = 1, cols = 4)
        val metrics = measureArrowEscapePuzzle(pieces, graph)
        assertEquals(3, metrics.maxFanOut)
        assertEquals(1, metrics.chokepointCount)
    }
}
