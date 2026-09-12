package com.quietgrid.engine.arrowescape

import com.quietgrid.engine.core.Difficulty
import kotlin.math.sqrt
import kotlin.random.Random

fun computeBottleneckScore(pieces: List<ArrowEscapePiece>, rows: Int, cols: Int): Double {
    val graph = buildDependencyGraph(pieces, rows, cols)
    var total = 0.0
    pieces.indices.forEach { index ->
        val blocked = graph.blocks[index]
        if (blocked.isNotEmpty()) {
            val distinctDirections = blocked.map { pieces[it].headDirection }.distinct().size
            total += (distinctDirections - 1) * blocked.size
        }
    }
    return total
}

fun computeNeedleScore(
    pieces: List<ArrowEscapePiece>,
    rows: Int,
    cols: Int,
    runs: Int = 6000,
    random: Random = Random.Default,
): Double {
    val graph = buildDependencyGraph(pieces, rows, cols)
    var total = 0.0
    repeat(runs) {
        total += simulateNeedleRun(pieces.size, graph, random)
    }
    return total / runs
}

private fun simulateNeedleRun(totalPieces: Int, graph: DependencyGraph, random: Random): Double {
    val removed = mutableSetOf<Int>()
    var runTotal = 0.0
    while (removed.size < totalPieces) {
        val candidates = (0 until totalPieces).filter { it !in removed && graph.dependsOn[it].all { dep -> dep in removed } }
        if (candidates.isEmpty()) return runTotal
        if (candidates.size == 1) runTotal += (totalPieces - removed.size).toDouble()
        removed.add(candidates[random.nextInt(candidates.size)])
    }
    return runTotal
}

data class ArrowEscapeDifficultyScore(
    val bottleneckScore: Double,
    val needleScore: Double,
    val normalizedScore: Double,
)

fun scoreArrowEscapePuzzle(pieces: List<ArrowEscapePiece>, rows: Int, cols: Int): ArrowEscapeDifficultyScore {
    val bottleneckScore = computeBottleneckScore(pieces, rows, cols)
    val needleScore = computeNeedleScore(pieces, rows, cols)
    return ArrowEscapeDifficultyScore(
        bottleneckScore = bottleneckScore,
        needleScore = needleScore,
        normalizedScore = (bottleneckScore + needleScore) / sqrt(pieces.size.toDouble()),
    )
}

data class ArrowEscapeScoreBoundaries(val mediumFloor: Double, val hardFloor: Double, val expertFloor: Double)

val ARROW_ESCAPE_SCORE_BOUNDARIES = ArrowEscapeScoreBoundaries(mediumFloor = 0.4443, hardFloor = 0.4880, expertFloor = 0.6321)

fun ArrowEscapeDifficultyScore.matchesDifficulty(
    target: Difficulty,
    boundaries: ArrowEscapeScoreBoundaries = ARROW_ESCAPE_SCORE_BOUNDARIES,
): Boolean = when (target) {
    Difficulty.EASY -> normalizedScore < boundaries.mediumFloor
    Difficulty.MEDIUM -> normalizedScore >= boundaries.mediumFloor && normalizedScore < boundaries.hardFloor
    Difficulty.HARD -> normalizedScore >= boundaries.hardFloor && normalizedScore < boundaries.expertFloor
    Difficulty.EXPERT -> normalizedScore >= boundaries.expertFloor
}
