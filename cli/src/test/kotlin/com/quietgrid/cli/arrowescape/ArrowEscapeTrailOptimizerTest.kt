package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.ARROW_DIRECTION_DELTA
import com.quietgrid.engine.arrowescape.ArrowEscapePiece
import com.quietgrid.engine.arrowescape.CellCoord
import com.quietgrid.engine.arrowescape.buildDependencyGraph
import com.quietgrid.engine.arrowescape.computeCorridor
import com.quietgrid.engine.arrowescape.profileArrowEscapePuzzle
import com.quietgrid.engine.arrowescape.solveArrowEscape
import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ArrowEscapeTrailOptimizerTest {
    private val size = 18
    private val params = ArrowEscapeTrailOptimizerParams(iterations = 1500)
    private val fill = (0 until 200).firstNotNullOf { seed ->
        fillCoverage(size, size, ARROW_ESCAPE_FILL_PARAMS_BY_DIFFICULTY.getValue(Difficulty.EXPERT), 0.03, Random(seed))
    }
    private val optimized = reduceArrowEscapeTrail(fill.pieces, size, size, params, Random(1))

    @Test
    fun `optimizing lowers the trail share`() {
        val before = profileArrowEscapePuzzle(fill.pieces, size, size).trailShare
        val after = profileArrowEscapePuzzle(optimized, size, size).trailShare
        assertTrue("trail share went from $before to $after", after < before)
    }

    @Test
    fun `optimized boards keep every cell, stay solvable and keep the piece count in range`() {
        assertEquals(fill.pieces.flatMap { it.cells }.toSet(), optimized.flatMap { it.cells }.toSet())
        assertEquals(optimized.sumOf { it.cells.size }, optimized.flatMap { it.cells }.toSet().size)
        assertTrue(solveArrowEscape(optimized, size, size).solvable)
        assertTrue(optimized.size >= fill.pieces.size * params.minPieceRatio)
        assertTrue(optimized.size <= fill.pieces.size * params.maxPieceRatio)
    }

    @Test
    fun `optimizing never adds arrows that are free from the start`() {
        val freeAtStart = { pieces: List<ArrowEscapePiece> ->
            buildDependencyGraph(pieces, size, size).dependsOn.count { it.isEmpty() }
        }
        assertTrue(freeAtStart(optimized) <= freeAtStart(fill.pieces))
    }

    @Test
    fun `optimized pieces are connected, enter their head straight and never point into their own body`() {
        optimized.forEach { piece ->
            assertTrue(piece.cells.size in 2..params.maxPieceLength)
            piece.cells.zipWithNext().forEach { (a, b) -> assertEquals(1, kotlin.math.abs(a.row - b.row) + kotlin.math.abs(a.col - b.col)) }
            val head = piece.cells.last()
            val (dr, dc) = ARROW_DIRECTION_DELTA.getValue(piece.headDirection)
            assertEquals(CellCoord(head.row - dr, head.col - dc), piece.cells[piece.cells.size - 2])
            assertTrue(computeCorridor(head.row, head.col, piece.headDirection, size, size).none { it in piece.cells })
        }
    }
}
