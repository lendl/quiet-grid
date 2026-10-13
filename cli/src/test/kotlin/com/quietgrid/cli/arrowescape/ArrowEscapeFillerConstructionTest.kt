// cli/src/test/kotlin/com/quietgrid/cli/arrowescape/ArrowEscapeFillerConstructionTest.kt
package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.ARROW_DIRECTION_DELTA
import com.quietgrid.engine.arrowescape.ArrowEscapePiece
import com.quietgrid.engine.arrowescape.CellCoord
import com.quietgrid.engine.arrowescape.computeCorridor
import com.quietgrid.engine.arrowescape.profileArrowEscapePuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil
import kotlin.random.Random

private val TEST_PARAMS = ArrowEscapeFillParams(borderExitRate = 0.1, depthGreed = 0.4)

private fun firstSuccessfulFill(rows: Int, cols: Int, tolerance: Double = 0.03): FillResult {
    for (seed in 0 until 200) {
        val result = fillCoverage(rows, cols, TEST_PARAMS, tolerance, Random(seed))
        if (result != null) return result
    }
    error("no successful fill found within 200 seeds")
}

private fun fillerPiecesAcrossSeeds(size: Int): List<ArrowEscapePiece> = (0 until 40).mapNotNull { seed ->
    fillCoverage(size, size, TEST_PARAMS, 0.03, Random(seed))
}.flatMap { it.pieces }

private fun turnRatio(piece: ArrowEscapePiece): Double {
    val steps = piece.cells.zipWithNext { a, b -> (b.row - a.row) to (b.col - a.col) }
    val turns = steps.zipWithNext().count { (a, b) -> a != b }
    return turns.toDouble() / (steps.size - 1)
}

private fun meanOverSeeds(params: ArrowEscapeFillParams, metric: (FillResult) -> Double) =
    (0 until 10).mapNotNull { fillCoverage(16, 16, params, 0.03, Random(it)) }.map(metric).average()

private fun borderExitShare(result: FillResult) =
    profileArrowEscapePuzzle(result.pieces, 16, 16, runs = 1).borderExitShare

class ArrowEscapeFillerConstructionTest {
    @Test
    fun `filler pieces mostly run straight instead of wiggling`() {
        val pieces = fillerPiecesAcrossSeeds(16).filter { it.cells.size >= 3 }
        val meanTurnRatio = pieces.map(::turnRatio).average()
        assertTrue("mean turn ratio was $meanTurnRatio", meanTurnRatio <= 0.3)
    }

    @Test
    fun `every piece enters its head straight and has at least two cells`() {
        val pieces = fillerPiecesAcrossSeeds(16)
        pieces.forEach { piece ->
            assertTrue(piece.cells.size >= 2)
            val head = piece.cells.last()
            val (dr, dc) = ARROW_DIRECTION_DELTA.getValue(piece.headDirection)
            assertEquals(CellCoord(head.row - dr, head.col - dc), piece.cells[piece.cells.size - 2])
        }
    }

    @Test
    fun `fillCoverage on an empty board covers every cell within the empty-cell tolerance`() {
        val result = firstSuccessfulFill(12, 10)
        val covered = result.pieces.sumOf { it.cells.size }
        assertEquals(12 * 10, covered + result.emptyCellCount)
        assertTrue(result.emptyCellCount <= ceil(12 * 10 * 0.03).toInt())
    }

    @Test
    fun `every filler piece is immediately removable given only earlier filler pieces`() {
        val result = firstSuccessfulFill(10, 10)
        val removedSoFar = mutableSetOf<Pair<Int, Int>>()
        for (piece in result.pieces) {
            val head = piece.cells.last()
            val corridor = computeCorridor(head.row, head.col, piece.headDirection, 10, 10)
            assertTrue(corridor.all { (it.row to it.col) in removedSoFar })
            piece.cells.forEach { removedSoFar.add(it.row to it.col) }
        }
    }

    @Test
    fun `border exit rate controls how many heads point straight off the board`() {
        assertTrue(meanOverSeeds(ArrowEscapeFillParams(0.05, 0.4), ::borderExitShare) <= 0.12)
        assertTrue(meanOverSeeds(ArrowEscapeFillParams(1.0, 0.0), ::borderExitShare) >= 0.25)
    }

    @Test
    fun `depth greed lengthens the removal order`() {
        val depthRatio = { result: FillResult ->
            profileArrowEscapePuzzle(result.pieces, 16, 16, runs = 1).let { it.rounds.toDouble() / it.pieceCount }
        }
        assertTrue(meanOverSeeds(ArrowEscapeFillParams(0.05, 0.7), depthRatio) > meanOverSeeds(ArrowEscapeFillParams(0.05, 0.0), depthRatio))
    }

    @Test
    fun `a zero border exit rate still fills an empty board`() {
        val result = (0 until 200).firstNotNullOf { fillCoverage(10, 10, ArrowEscapeFillParams(0.0, 0.4), 0.03, Random(it)) }
        assertTrue(profileArrowEscapePuzzle(result.pieces, 10, 10, runs = 1).borderExitShare > 0.0)
    }
}
