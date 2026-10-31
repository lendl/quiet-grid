package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.ARROW_DIRECTION_DELTA
import com.quietgrid.engine.arrowescape.ArrowDirection
import com.quietgrid.engine.arrowescape.ArrowEscapePiece
import com.quietgrid.engine.arrowescape.CellCoord
import com.quietgrid.engine.arrowescape.buildDependencyGraph
import com.quietgrid.engine.arrowescape.computeCorridor
import com.quietgrid.engine.arrowescape.simulateTrailShare
import kotlin.random.Random

data class ArrowEscapeTrailOptimizerParams(
    val iterations: Int,
    val evaluationRuns: Int = 8,
    val minPieceRatio: Double = 0.85,
    val maxPieceRatio: Double = 1.15,
    val maxPieceLength: Int = 45,
)

val ARROW_ESCAPE_TRAIL_OPTIMIZER_PARAMS = ArrowEscapeTrailOptimizerParams(iterations = 15000)

private const val FLIP_SHARE = 0.4
private const val SPLIT_SHARE = 0.3

fun reduceArrowEscapeTrail(
    pieces: List<ArrowEscapePiece>,
    rows: Int,
    cols: Int,
    params: ArrowEscapeTrailOptimizerParams,
    random: Random,
    minPieces: Int = 0,
): List<ArrowEscapePiece> {
    val pieceRange = maxOf(pieces.size * params.minPieceRatio, minPieces.toDouble())..(pieces.size * params.maxPieceRatio)
    val maxFreeAtStart = buildDependencyGraph(pieces, rows, cols).dependsOn.count { it.isEmpty() }
    var current = pieces
    var currentTrail = evaluateTrail(current, rows, cols, params.evaluationRuns, maxFreeAtStart) ?: return pieces
    repeat(params.iterations) {
        val proposal = proposeTrailMove(current, rows, cols, params.maxPieceLength, random) ?: return@repeat
        if (proposal.size.toDouble() !in pieceRange) return@repeat
        val trail = evaluateTrail(proposal, rows, cols, params.evaluationRuns, maxFreeAtStart) ?: return@repeat
        if (trail <= currentTrail) {
            current = proposal
            currentTrail = trail
        }
    }
    return current
}

private fun evaluateTrail(pieces: List<ArrowEscapePiece>, rows: Int, cols: Int, runs: Int, maxFreeAtStart: Int): Double? {
    val graph = buildDependencyGraph(pieces, rows, cols)
    val pending = IntArray(pieces.size) { graph.dependsOn[it].size }
    val ready = ArrayDeque(pieces.indices.filter { pending[it] == 0 })
    if (ready.size > maxFreeAtStart) return null
    var cleared = 0
    while (ready.isNotEmpty()) {
        val next = ready.removeFirst()
        cleared += 1
        graph.blocks[next].forEach { blocked ->
            pending[blocked] -= 1
            if (pending[blocked] == 0) ready.addLast(blocked)
        }
    }
    if (cleared != pieces.size) return null
    return simulateTrailShare(pieces, graph, rows, cols, runs, Random(0))
}

private fun proposeTrailMove(
    pieces: List<ArrowEscapePiece>,
    rows: Int,
    cols: Int,
    maxPieceLength: Int,
    random: Random,
): List<ArrowEscapePiece>? {
    val index = random.nextInt(pieces.size)
    val piece = pieces[index]
    val roll = random.nextDouble()
    return when {
        roll < FLIP_SHARE -> pieceFromCells(piece.cells.reversed(), rows, cols)?.let { pieces.replaced(index, listOf(it)) }
        roll < FLIP_SHARE + SPLIT_SHARE -> splitPiece(piece, rows, cols, random)?.let { pieces.replaced(index, it) }
        else -> mergeIntoTail(pieces, index, rows, cols, maxPieceLength, random)
    }
}

private fun splitPiece(piece: ArrowEscapePiece, rows: Int, cols: Int, random: Random): List<ArrowEscapePiece>? {
    if (piece.cells.size < 4) return null
    val cut = 2 + random.nextInt(piece.cells.size - 3)
    val front = pieceFromCells(piece.cells.subList(0, cut), rows, cols) ?: return null
    val back = ArrowEscapePiece(piece.cells.subList(cut, piece.cells.size), piece.headDirection)
    return listOf(front, back)
}

private fun mergeIntoTail(
    pieces: List<ArrowEscapePiece>,
    index: Int,
    rows: Int,
    cols: Int,
    maxPieceLength: Int,
    random: Random,
): List<ArrowEscapePiece>? {
    val head = pieces[index].cells.last()
    val partners = pieces.indices.filter { other ->
        other != index && pieces[other].cells.first().let { tail -> isAdjacent(head, tail) }
    }
    if (partners.isEmpty()) return null
    val partner = partners[random.nextInt(partners.size)]
    val cells = pieces[index].cells + pieces[partner].cells
    if (cells.size > maxPieceLength) return null
    val merged = ArrowEscapePiece(cells, pieces[partner].headDirection)
    if (pointsIntoOwnBody(merged, rows, cols)) return null
    return pieces.filterIndexed { i, _ -> i != index && i != partner } + merged
}

private fun pieceFromCells(cells: List<CellCoord>, rows: Int, cols: Int): ArrowEscapePiece? {
    if (cells.size < 2) return null
    val neck = cells[cells.size - 2]
    val head = cells.last()
    val direction = ArrowDirection.entries.first { ARROW_DIRECTION_DELTA.getValue(it) == (head.row - neck.row to head.col - neck.col) }
    val piece = ArrowEscapePiece(cells, direction)
    return if (pointsIntoOwnBody(piece, rows, cols)) null else piece
}

private fun pointsIntoOwnBody(piece: ArrowEscapePiece, rows: Int, cols: Int): Boolean {
    val head = piece.cells.last()
    val body = piece.cells.toSet()
    return computeCorridor(head.row, head.col, piece.headDirection, rows, cols).any { it in body }
}

private fun isAdjacent(a: CellCoord, b: CellCoord): Boolean =
    kotlin.math.abs(a.row - b.row) + kotlin.math.abs(a.col - b.col) == 1

private fun List<ArrowEscapePiece>.replaced(index: Int, replacement: List<ArrowEscapePiece>): List<ArrowEscapePiece> =
    subList(0, index) + replacement + subList(index + 1, size)
