// cli/src/test/kotlin/com/quietgrid/cli/arrowescape/ArrowEscapeFillerConstructionTest.kt
package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.ARROW_DIRECTION_DELTA
import com.quietgrid.engine.arrowescape.ArrowEscapePiece
import com.quietgrid.engine.arrowescape.CellCoord
import com.quietgrid.engine.arrowescape.computeCorridor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil
import kotlin.random.Random

private fun firstSuccessfulFill(rows: Int, cols: Int, baseOccupied: Set<Pair<Int, Int>>, tolerance: Double = 0.03): Pair<FillResult, MutableSet<Pair<Int, Int>>> {
    for (seed in 0 until 200) {
        val occupied = baseOccupied.toMutableSet()
        val result = fillCoverage(rows, cols, occupied, tolerance, Random(seed))
        if (result != null) return result to occupied
    }
    error("no successful fill found within 200 seeds")
}

private fun fillerPiecesAcrossSeeds(size: Int): List<ArrowEscapePiece> = (0 until 40).mapNotNull { seed ->
    fillCoverage(size, size, mutableSetOf(), 0.03, Random(seed))
}.flatMap { it.pieces }

private fun turnRatio(piece: ArrowEscapePiece): Double {
    val steps = piece.cells.zipWithNext { a, b -> (b.row - a.row) to (b.col - a.col) }
    val turns = steps.zipWithNext().count { (a, b) -> a != b }
    return turns.toDouble() / (steps.size - 1)
}

class ArrowEscapeFillerConstructionTest {
    @Test
    fun `filler pieces mostly run straight instead of wiggling`() {
        val pieces = fillerPiecesAcrossSeeds(16).filter { it.cells.size >= 3 }
        val meanTurnRatio = pieces.map(::turnRatio).average()
        assertTrue("mean turn ratio was $meanTurnRatio", meanTurnRatio <= 0.3)
    }

    @Test
    fun `filler pieces enter their head straight along the head direction`() {
        val pieces = fillerPiecesAcrossSeeds(16).filter { it.cells.size >= 2 }
        val straightShafts = pieces.count { piece ->
            val head = piece.cells.last()
            val beforeHead = piece.cells[piece.cells.size - 2]
            val (dr, dc) = ARROW_DIRECTION_DELTA.getValue(piece.headDirection)
            beforeHead == CellCoord(head.row - dr, head.col - dc)
        }
        val ratio = straightShafts.toDouble() / pieces.size
        assertTrue("straight shaft ratio was $ratio", ratio >= 0.8)
    }

    @Test
    fun `fillCoverage on an empty board covers every cell within the empty-cell tolerance`() {
        val (result, _) = firstSuccessfulFill(12, 10, emptySet())
        val covered = result.pieces.sumOf { it.cells.size }
        assertEquals(12 * 10, covered + result.emptyCellCount)
        assertTrue(result.emptyCellCount <= ceil(12 * 10 * 0.03).toInt())
    }

    @Test
    fun `every filler piece is immediately removable given only earlier filler pieces`() {
        val (result, _) = firstSuccessfulFill(10, 10, emptySet())
        val removedSoFar = mutableSetOf<Pair<Int, Int>>()
        for (piece in result.pieces) {
            val head = piece.cells.last()
            val corridor = computeCorridor(head.row, head.col, piece.headDirection, 10, 10)
            assertTrue(corridor.all { (it.row to it.col) in removedSoFar })
            piece.cells.forEach { removedSoFar.add(it.row to it.col) }
        }
    }

    @Test
    fun `fillCoverage respects pre-occupied cells and does not double-cover them`() {
        val (result, _) = firstSuccessfulFill(8, 8, setOf(2 to 2, 2 to 3))
        val fillerCells = result.pieces.flatMap { it.cells }.map { it.row to it.col }.toSet()
        assertTrue((2 to 2) !in fillerCells)
        assertTrue((2 to 3) !in fillerCells)
    }

    @Test
    fun `fillCoverage adds every carved filler cell to the occupied set`() {
        val (result, occupied) = firstSuccessfulFill(8, 8, setOf(1 to 1))
        result.pieces.forEach { piece -> piece.cells.forEach { assertTrue((it.row to it.col) in occupied) } }
    }
}
