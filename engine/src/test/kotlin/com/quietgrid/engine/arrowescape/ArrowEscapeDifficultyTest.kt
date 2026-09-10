// engine/src/test/kotlin/com/quietgrid/engine/arrowescape/ArrowEscapeDifficultyTest.kt
package com.quietgrid.engine.arrowescape

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowEscapeDifficultyTest {
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
