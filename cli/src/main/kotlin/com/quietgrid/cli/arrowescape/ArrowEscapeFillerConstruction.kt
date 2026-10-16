// cli/src/main/kotlin/com/quietgrid/cli/arrowescape/ArrowEscapeFillerConstruction.kt
package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.ARROW_DIRECTION_DELTA
import com.quietgrid.engine.arrowescape.ArrowDirection
import com.quietgrid.engine.arrowescape.ArrowEscapePiece
import com.quietgrid.engine.arrowescape.CellCoord
import com.quietgrid.engine.arrowescape.computeCorridor
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random

private const val STRAIGHT_PREFERENCE = 0.9
private const val MIN_TARGET_LEN = 3
private const val MAX_TARGET_LEN = 45
private const val DEEP_FILL_SAMPLE_SIZE = 200

private val OPPOSITE_DIRECTION: Map<ArrowDirection, ArrowDirection> = mapOf(
    ArrowDirection.UP to ArrowDirection.DOWN,
    ArrowDirection.DOWN to ArrowDirection.UP,
    ArrowDirection.LEFT to ArrowDirection.RIGHT,
    ArrowDirection.RIGHT to ArrowDirection.LEFT,
)

data class FillResult(val pieces: List<ArrowEscapePiece>, val emptyCellCount: Int)

data class ArrowEscapeFillParams(val borderExitRate: Double, val depthGreed: Double)

private data class DeepFillCandidate(
    val cell: Pair<Int, Int>,
    val direction: ArrowDirection,
    val depth: Int,
    val activeCorridorLength: Int,
)

fun fillCoverage(
    rows: Int,
    cols: Int,
    params: ArrowEscapeFillParams,
    maxEmptyRatio: Double,
    random: Random = Random.Default,
    activeCells: Set<Pair<Int, Int>>? = null,
): FillResult? {
    val active = activeCells ?: buildSet { for (r in 0 until rows) for (c in 0 until cols) add(r to c) }
    val remaining = active.toMutableSet()
    val owner = mutableMapOf<Pair<Int, Int>, Int>()
    val depths = mutableListOf<Int>()
    val pieces = mutableListOf<ArrowEscapePiece>()

    while (remaining.isNotEmpty()) {
        val candidates = sampleDeepFillCandidates(rows, cols, active, remaining, owner, depths, random)
        if (candidates.isEmpty()) break
        val inner = candidates.filter { it.activeCorridorLength > 0 }
        val pool = if (inner.isNotEmpty() && random.nextDouble() >= params.borderExitRate) inner else candidates
        val chosen = if (random.nextDouble() < params.depthGreed) {
            pool.maxWith(compareBy<DeepFillCandidate> { it.depth }.thenByDescending { it.activeCorridorLength })
        } else {
            pool[random.nextInt(pool.size)]
        }
        val u = random.nextDouble()
        val targetLen = MIN_TARGET_LEN + floor((MAX_TARGET_LEN - MIN_TARGET_LEN + 1) * u * u).toInt()
        val cells = growTowardTail(rows, cols, chosen.cell, chosen.direction, targetLen, remaining, random)
        val pieceIndex = pieces.size
        cells.forEach {
            remaining.remove(it)
            owner[it] = pieceIndex
        }
        depths.add(chosen.depth)
        pieces.add(ArrowEscapePiece(cells = cells.map { CellCoord(it.first, it.second) }, headDirection = chosen.direction))
    }

    if (activeCells != null) absorbLeftovers(rows, cols, pieces, remaining, owner)

    val emptyCellCount = remaining.size
    if (emptyCellCount > ceil(active.size * maxEmptyRatio).toInt()) return null
    return FillResult(pieces, emptyCellCount)
}

private fun absorbLeftovers(
    rows: Int,
    cols: Int,
    pieces: MutableList<ArrowEscapePiece>,
    remaining: MutableSet<Pair<Int, Int>>,
    owner: MutableMap<Pair<Int, Int>, Int>,
) {
    var changed = true
    while (changed) {
        changed = false
        for (cell in remaining.sortedWith(compareBy({ it.first }, { it.second }))) {
            val coord = CellCoord(cell.first, cell.second)
            val tailIndex = neighborsOf(cell).firstNotNullOfOrNull { neighbor ->
                owner[neighbor]?.takeIf { pieces[it].cells.first() == CellCoord(neighbor.first, neighbor.second) }
            }
            if (tailIndex != null) {
                val piece = pieces[tailIndex]
                pieces[tailIndex] = piece.copy(cells = listOf(coord) + piece.cells)
                owner[cell] = tailIndex
            } else {
                val headExtension = ArrowDirection.entries.firstNotNullOfOrNull { direction ->
                    val (dr, dc) = ARROW_DIRECTION_DELTA.getValue(direction)
                    val neck = (cell.first - dr) to (cell.second - dc)
                    val pieceIndex = owner[neck] ?: return@firstNotNullOfOrNull null
                    if (pieces[pieceIndex].cells.last() != CellCoord(neck.first, neck.second)) return@firstNotNullOfOrNull null
                    val corridor = computeCorridor(cell.first, cell.second, direction, rows, cols)
                    val clear = corridor.all { next ->
                        val key = next.row to next.col
                        key !in remaining && (owner[key]?.let { it < pieceIndex } ?: true)
                    }
                    if (clear) pieceIndex to direction else null
                } ?: continue
                val (pieceIndex, direction) = headExtension
                val piece = pieces[pieceIndex]
                pieces[pieceIndex] = ArrowEscapePiece(cells = piece.cells + coord, headDirection = direction)
                owner[cell] = pieceIndex
            }
            remaining.remove(cell)
            changed = true
        }
    }
}

private fun neighborsOf(cell: Pair<Int, Int>): List<Pair<Int, Int>> =
    ARROW_DIRECTION_DELTA.values.map { (dr, dc) -> (cell.first + dr) to (cell.second + dc) }

private fun sampleDeepFillCandidates(
    rows: Int,
    cols: Int,
    active: Set<Pair<Int, Int>>,
    remaining: Set<Pair<Int, Int>>,
    owner: Map<Pair<Int, Int>, Int>,
    depths: List<Int>,
    random: Random,
): List<DeepFillCandidate> {
    val remainingList = remaining.toList()
    val startIdx = random.nextInt(remainingList.size)
    val candidates = mutableListOf<DeepFillCandidate>()
    for (off in remainingList.indices) {
        if (candidates.size >= DEEP_FILL_SAMPLE_SIZE) break
        val cell = remainingList[(startIdx + off) % remainingList.size]
        for (direction in ArrowDirection.entries) {
            val (dr, dc) = ARROW_DIRECTION_DELTA.getValue(direction)
            val neck = (cell.first - dr) to (cell.second - dc)
            if (neck !in remaining) continue
            val corridor = computeCorridor(cell.first, cell.second, direction, rows, cols)
            if (corridor.any { (it.row to it.col) in remaining }) continue
            val depth = corridor.mapNotNull { owner[it.row to it.col] }.maxOfOrNull { depths[it] + 1 } ?: 0
            candidates.add(DeepFillCandidate(cell, direction, depth, corridor.count { (it.row to it.col) in active }))
        }
    }
    return candidates
}

private fun growTowardTail(
    rows: Int,
    cols: Int,
    head: Pair<Int, Int>,
    headDirection: ArrowDirection,
    targetLen: Int,
    remaining: Set<Pair<Int, Int>>,
    random: Random,
): List<Pair<Int, Int>> {
    val cellsTailToHead = mutableListOf(head)
    val inPiece = mutableSetOf(head)
    var current = head
    var growthDirection = OPPOSITE_DIRECTION.getValue(headDirection)
    var isShaftStep = true
    while (cellsTailToHead.size < targetLen) {
        val options = ArrowDirection.entries.mapNotNull { candidateDirection ->
            val (dr, dc) = ARROW_DIRECTION_DELTA.getValue(candidateDirection)
            val next = (current.first + dr) to (current.second + dc)
            if (next.first !in 0 until rows || next.second !in 0 until cols) return@mapNotNull null
            if (next !in remaining || next in inPiece) return@mapNotNull null
            candidateDirection to next
        }
        if (options.isEmpty()) break
        val straight = options.firstOrNull { it.first == growthDirection }
        val keepStraight = straight != null && (isShaftStep || random.nextDouble() < STRAIGHT_PREFERENCE)
        val (direction, next) = if (keepStraight) straight else options[random.nextInt(options.size)]
        cellsTailToHead.add(0, next)
        inPiece.add(next)
        current = next
        growthDirection = direction
        isShaftStep = false
    }
    return cellsTailToHead
}
