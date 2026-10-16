package com.quietgrid.engine.arrowescape

import com.quietgrid.engine.core.Difficulty
import kotlin.random.Random

data class ArrowEscapeDifficultyProfile(
    val pieceCount: Int,
    val rounds: Int,
    val freeShare: Double,
    val borderExitShare: Double,
    val meanBlockers: Double,
)

data class ArrowEscapeFreeShareFloors(val easy: Double, val medium: Double, val hard: Double)

val ARROW_ESCAPE_FREE_SHARE_FLOORS = ArrowEscapeFreeShareFloors(easy = 0.52, medium = 0.40, hard = 0.29)

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
    )
}

fun classifyArrowEscapeDifficulty(
    profile: ArrowEscapeDifficultyProfile,
    floors: ArrowEscapeFreeShareFloors = ARROW_ESCAPE_FREE_SHARE_FLOORS,
): Difficulty = when {
    profile.freeShare >= floors.easy -> Difficulty.EASY
    profile.freeShare >= floors.medium -> Difficulty.MEDIUM
    profile.freeShare >= floors.hard -> Difficulty.HARD
    else -> Difficulty.EXPERT
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
