// cli/src/main/kotlin/com/quietgrid/cli/arrowescape/ArrowEscapeFillerConstruction.kt
package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.ARROW_DIRECTION_DELTA
import com.quietgrid.engine.arrowescape.ArrowDirection
import com.quietgrid.engine.arrowescape.ArrowEscapePiece
import com.quietgrid.engine.arrowescape.CellCoord
import com.quietgrid.engine.arrowescape.computeCorridor
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random

private const val MIN_FILLER_LEN = 10
private const val MAX_FILLER_LEN = 25
private const val MIN_ACCEPTABLE_PIECE_LEN = 4
private const val CANDIDATE_SAMPLE_SIZE = 60

data class FillResult(val pieces: List<ArrowEscapePiece>, val emptyCellCount: Int)

private fun isAdjacent(a: Pair<Int, Int>, b: Pair<Int, Int>): Boolean {
    val dr = abs(a.first - b.first)
    val dc = abs(a.second - b.second)
    return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
}

private fun tryMergeOrphanIntoExistingPiece(pieces: MutableList<ArrowEscapePiece>, orphan: List<Pair<Int, Int>>): Boolean {
    for (index in pieces.indices) {
        val piece = pieces[index]
        val tail = piece.cells.first().row to piece.cells.first().col
        val newCells = when {
            isAdjacent(orphan.last(), tail) -> orphan.map { CellCoord(it.first, it.second) } + piece.cells
            isAdjacent(orphan.first(), tail) -> orphan.reversed().map { CellCoord(it.first, it.second) } + piece.cells
            else -> null
        }
        if (newCells != null) {
            pieces[index] = piece.copy(cells = newCells)
            return true
        }
    }
    return false
}

fun fillCoverage(
    rows: Int,
    cols: Int,
    occupied: MutableSet<Pair<Int, Int>>,
    maxEmptyRatio: Double,
    random: Random = Random.Default,
): FillResult? {
    val totalCells = rows * cols
    val remaining = mutableSetOf<Pair<Int, Int>>()
    for (r in 0 until rows) for (c in 0 until cols) if ((r to c) !in occupied) remaining.add(r to c)

    val pieces = mutableListOf<ArrowEscapePiece>()
    val directions = ArrowDirection.entries
    var permanentlyUncovered = 0

    while (remaining.isNotEmpty()) {
        val remainingList = remaining.toList()
        val startIdx = random.nextInt(remainingList.size)
        var bestCell: Pair<Int, Int>? = null
        var bestDirection: ArrowDirection? = null
        var bestCorridorLength = Int.MAX_VALUE
        var examined = 0

        for (off in remainingList.indices) {
            if (examined >= CANDIDATE_SAMPLE_SIZE) break
            val cellPair = remainingList[(startIdx + off) % remainingList.size]
            for (direction in directions) {
                val corridor = computeCorridor(cellPair.first, cellPair.second, direction, rows, cols)
                if (corridor.all { (it.row to it.col) !in remaining }) {
                    examined += 1
                    if (corridor.size < bestCorridorLength) {
                        bestCell = cellPair
                        bestDirection = direction
                        bestCorridorLength = corridor.size
                    }
                }
            }
        }

        if (bestCell == null || bestDirection == null) break

        val targetLen = MIN_FILLER_LEN + random.nextInt(MAX_FILLER_LEN - MIN_FILLER_LEN + 1)
        val cellsTailToHead = mutableListOf(bestCell!!)
        val inPiece = mutableSetOf(bestCell!!)
        var current = bestCell!!

        while (cellsTailToHead.size < targetLen) {
            val options = mutableListOf<Pair<Int, Int>>()
            for (candidateDirection in directions) {
                val (dr, dc) = ARROW_DIRECTION_DELTA.getValue(candidateDirection)
                val next = (current.first + dr) to (current.second + dc)
                if (next.first !in 0 until rows || next.second !in 0 until cols) continue
                if (next !in remaining || next in inPiece) continue
                options.add(next)
            }
            if (options.isEmpty()) break
            val next = options[random.nextInt(options.size)]
            cellsTailToHead.add(0, next)
            inPiece.add(next)
            current = next
        }

        if (cellsTailToHead.size < MIN_ACCEPTABLE_PIECE_LEN) {
            val merged = tryMergeOrphanIntoExistingPiece(pieces, cellsTailToHead)
            if (!merged) permanentlyUncovered += cellsTailToHead.size
            cellsTailToHead.forEach { remaining.remove(it); occupied.add(it) }
        } else {
            cellsTailToHead.forEach { remaining.remove(it); occupied.add(it) }
            pieces.add(ArrowEscapePiece(cells = cellsTailToHead.map { CellCoord(it.first, it.second) }, headDirection = bestDirection))
        }
    }

    val emptyCellCount = remaining.size + permanentlyUncovered
    if (emptyCellCount > ceil(totalCells * maxEmptyRatio).toInt()) return null

    return FillResult(pieces, emptyCellCount)
}
