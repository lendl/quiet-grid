package com.quietgrid.engine.arrowescape

import com.quietgrid.engine.core.Difficulty
import kotlin.random.Random

data class ArrowEscapeDifficultyProfile(
    val pieceCount: Int,
    val rounds: Int,
    val freeShare: Double,
    val borderExitShare: Double,
    val meanBlockers: Double,
    val trailShare: Double,
)

data class ArrowEscapeFreeShareFloors(val easy: Double, val medium: Double)

val ARROW_ESCAPE_FREE_SHARE_FLOORS = ArrowEscapeFreeShareFloors(easy = 0.52, medium = 0.29)

const val ARROW_ESCAPE_EXPERT_TRAIL_CEILING = 0.25

const val ARROW_ESCAPE_EXPERT_FREE_SHARE_CEILING = 0.15

const val ARROW_ESCAPE_TRAIL_SIGHT = 3

fun profileArrowEscapePuzzle(
    pieces: List<ArrowEscapePiece>,
    rows: Int,
    cols: Int,
    runs: Int = 100,
    random: Random = Random(0),
    activeCells: Set<CellCoord>? = null,
): ArrowEscapeDifficultyProfile {
    val graph = buildDependencyGraph(pieces, rows, cols)
    val blockerCounts = graph.dependsOn.map { it.size }.filter { it > 0 }
    val borderExits = pieces.count { piece ->
        val head = piece.cells.last()
        computeCorridor(head.row, head.col, piece.headDirection, rows, cols).none { activeCells == null || it in activeCells }
    }
    return ArrowEscapeDifficultyProfile(
        pieceCount = pieces.size,
        rounds = countRemovalRounds(graph, pieces.size),
        freeShare = simulateFreeShare(graph, pieces.size, runs, random),
        borderExitShare = if (pieces.isEmpty()) 0.0 else borderExits.toDouble() / pieces.size,
        meanBlockers = if (blockerCounts.isEmpty()) 0.0 else blockerCounts.average(),
        trailShare = simulateTrailShare(pieces, graph, rows, cols, runs, random),
    )
}

fun classifyArrowEscapeDifficulty(
    profile: ArrowEscapeDifficultyProfile,
    floors: ArrowEscapeFreeShareFloors = ARROW_ESCAPE_FREE_SHARE_FLOORS,
    expertTrailCeiling: Double = ARROW_ESCAPE_EXPERT_TRAIL_CEILING,
    expertFreeShareCeiling: Double = ARROW_ESCAPE_EXPERT_FREE_SHARE_CEILING,
): Difficulty = when {
    profile.freeShare >= floors.easy -> Difficulty.EASY
    profile.freeShare >= floors.medium -> Difficulty.MEDIUM
    profile.freeShare >= expertFreeShareCeiling -> Difficulty.HARD
    profile.trailShare > expertTrailCeiling -> Difficulty.HARD
    else -> Difficulty.EXPERT
}

fun simulateTrailShare(
    pieces: List<ArrowEscapePiece>,
    graph: DependencyGraph,
    rows: Int,
    cols: Int,
    runs: Int,
    random: Random,
): Double {
    val owner = buildCellOwnerGrid(pieces, rows, cols)
    val sightOwners = pieces.map { corridorOwners(it, owner, rows, cols, ARROW_ESCAPE_TRAIL_SIGHT).toIntArray() }
    val pending = IntArray(pieces.size)
    val free = IntArray(pieces.size)
    val trail = IntArray(pieces.size)
    var trailMoves = 0
    var moves = 0
    repeat(runs) {
        var freeCount = 0
        for (index in pieces.indices) {
            pending[index] = graph.dependsOn[index].size
            if (pending[index] == 0) free[freeCount++] = index
        }
        var last = -1
        while (freeCount > 0) {
            var trailCount = 0
            if (last >= 0) {
                for (slot in 0 until freeCount) {
                    if (last in sightOwners[free[slot]]) trail[trailCount++] = free[slot]
                }
            }
            val pick = if (trailCount > 0) trail[random.nextInt(trailCount)] else free[random.nextInt(freeCount)]
            if (trailCount > 0) trailMoves += 1
            moves += 1
            val slot = (0 until freeCount).first { free[it] == pick }
            free.copyInto(free, slot, slot + 1, freeCount)
            freeCount--
            last = pick
            graph.blocks[pick].forEach { blocked ->
                pending[blocked] -= 1
                if (pending[blocked] == 0) free[freeCount++] = blocked
            }
        }
    }
    return if (moves == 0) 0.0 else trailMoves.toDouble() / moves
}

private fun freePieces(graph: DependencyGraph, totalPieces: Int, removed: Set<Int>): List<Int> =
    (0 until totalPieces).filter { it !in removed && graph.dependsOn[it].all { dep -> dep in removed } }

private fun countRemovalRounds(graph: DependencyGraph, totalPieces: Int): Int {
    val removed = mutableSetOf<Int>()
    var rounds = 0
    while (removed.size < totalPieces) {
        val free = freePieces(graph, totalPieces, removed)
        if (free.isEmpty()) break
        removed.addAll(free)
        rounds += 1
    }
    return rounds
}

private fun simulateFreeShare(graph: DependencyGraph, totalPieces: Int, runs: Int, random: Random): Double {
    var sum = 0.0
    var samples = 0
    repeat(runs) {
        val removed = mutableSetOf<Int>()
        while (removed.size < totalPieces) {
            val free = freePieces(graph, totalPieces, removed)
            sum += free.size.toDouble() / (totalPieces - removed.size)
            samples += 1
            if (free.isEmpty()) break
            removed.add(free[random.nextInt(free.size)])
        }
    }
    return if (samples == 0) 0.0 else sum / samples
}
