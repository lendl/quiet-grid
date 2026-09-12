// cli/src/test/kotlin/com/quietgrid/cli/arrowescape/ArrowEscapeGeneratorTest.kt
package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.ArrowEscapeSolveTier
import com.quietgrid.engine.arrowescape.matchesDifficulty
import com.quietgrid.engine.arrowescape.scoreArrowEscapePuzzle
import com.quietgrid.engine.arrowescape.solveArrowEscape
import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private fun firstSuccessfulFill(rows: Int, cols: Int, startSeed: Int) = generateSequence(startSeed) { it + 1000 }
    .take(200)
    .firstNotNullOf { seed -> fillCoverage(rows, cols, mutableSetOf(), 0.03, Random(seed)) }

private fun firstSuccessfulGeneration(rows: Int, cols: Int, difficulty: Difficulty, startSeed: Int) = generateSequence(startSeed) { it + 1000 }
    .take(200)
    .firstNotNullOf { seed -> generateArrowEscapePuzzle(rows, cols, difficulty, Random(seed)) }

class ArrowEscapeGeneratorTest {
    @Test
    fun `buildPuzzleFingerprint is stable for the same pieces and unaffected by order`() {
        val result = firstSuccessfulFill(10, 10, 101)
        val fp1 = buildPuzzleFingerprint(result.pieces)
        val fp2 = buildPuzzleFingerprint(result.pieces)
        assertEquals(fp1, fp2)
        assertEquals(fp1, buildPuzzleFingerprint(result.pieces.reversed()))
    }

    @Test
    fun `buildPuzzleFingerprint differs for different generations`() {
        val fp1 = buildPuzzleFingerprint(firstSuccessfulFill(10, 10, 101).pieces)
        val fp2 = buildPuzzleFingerprint(firstSuccessfulFill(10, 10, 202).pieces)
        assertNotEquals(fp1, fp2)
    }

    @Test
    fun `arrowEscapeSizesForDifficulty covers the documented ranges`() {
        assertEquals(listOf(10, 11, 12), arrowEscapeSizesForDifficulty(Difficulty.EASY))
        assertEquals((18..30).toList(), arrowEscapeSizesForDifficulty(Difficulty.EXPERT))
    }

    @Test
    fun `generateArrowEscapePuzzle produces a full-coverage entry classified at the target difficulty`() {
        val result = firstSuccessfulGeneration(16, 16, Difficulty.HARD, 1)
        val covered = result.pieces.sumOf { it.cells.size }
        val allowedUncovered = kotlin.math.ceil(16 * 16 * ARROW_ESCAPE_EMPTY_CELL_TOLERANCE).toInt()
        assertTrue(covered >= 16 * 16 - allowedUncovered)
        assertEquals(Difficulty.HARD, result.difficulty)
        assertTrue(result.dedupeKey.isNotEmpty())
    }

    @Test
    fun `two independent calls produce different dedupe keys`() {
        val first = firstSuccessfulGeneration(13, 13, Difficulty.MEDIUM, 1)
        val second = firstSuccessfulGeneration(13, 13, Difficulty.MEDIUM, 2)
        assertNotEquals(first.dedupeKey, second.dedupeKey)
    }

    @Test
    fun `generateArrowEscapePuzzle produces a puzzle whose score matches the target difficulty under the v2 model`() {
        val result = firstSuccessfulGeneration(16, 16, Difficulty.HARD, 1)
        val score = scoreArrowEscapePuzzle(result.pieces, result.rows, result.cols)
        assertTrue(score.matchesDifficulty(Difficulty.HARD))
    }

    @Test
    fun `generated puzzles across all difficulties never require tier 5 or 6 reasoning`() {
        Difficulty.entries.forEach { difficulty ->
            val size = arrowEscapeSizesForDifficulty(difficulty).first()
            val generated = firstSuccessfulGeneration(size, size, difficulty, startSeed = difficulty.ordinal * 500 + 1)
            val result = solveArrowEscape(generated.pieces, generated.rows, generated.cols)
            assertTrue(result.solvable)
            assertTrue(result.highestTier!!.ordinal <= ArrowEscapeSolveTier.BOTTLENECK.ordinal)
        }
    }
}
