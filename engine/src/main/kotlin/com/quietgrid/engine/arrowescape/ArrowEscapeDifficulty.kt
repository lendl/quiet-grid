package com.quietgrid.engine.arrowescape

import com.quietgrid.engine.core.Difficulty

private const val CHOKEPOINT_FAN_OUT_THRESHOLD = 3

data class ArrowEscapeDifficultyMetrics(val maxFanOut: Int, val chokepointCount: Int)

fun measureArrowEscapePuzzle(pieces: List<ArrowEscapePiece>, graph: DependencyGraph): ArrowEscapeDifficultyMetrics {
    var maxFanOut = 0
    var chokepointCount = 0
    pieces.indices.forEach { index ->
        val fanOut = graph.blocks[index].size
        if (fanOut > maxFanOut) maxFanOut = fanOut
        if (fanOut >= CHOKEPOINT_FAN_OUT_THRESHOLD) chokepointCount += 1
    }
    return ArrowEscapeDifficultyMetrics(maxFanOut, chokepointCount)
}

fun classifyArrowEscapeDifficulty(metrics: ArrowEscapeDifficultyMetrics): Difficulty = when {
    metrics.chokepointCount == 0 -> Difficulty.EASY
    metrics.chokepointCount <= 2 -> Difficulty.MEDIUM
    metrics.chokepointCount <= 5 -> Difficulty.HARD
    else -> Difficulty.EXPERT
}

fun computeArrowEscapeScore(metrics: ArrowEscapeDifficultyMetrics): Int =
    metrics.maxFanOut * 100 + metrics.chokepointCount * 50

private val CHAIN_LENGTH_BY_DIFFICULTY: Map<Difficulty, Int> = mapOf(
    Difficulty.EASY to 2,
    Difficulty.MEDIUM to 3,
    Difficulty.HARD to 4,
    Difficulty.EXPERT to 6,
)

fun chainLengthForDifficulty(difficulty: Difficulty): Int = CHAIN_LENGTH_BY_DIFFICULTY.getValue(difficulty)
