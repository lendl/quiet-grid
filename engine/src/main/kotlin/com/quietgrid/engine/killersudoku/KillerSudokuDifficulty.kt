package com.quietgrid.engine.killersudoku

import com.quietgrid.engine.core.Difficulty

data class KillerDifficultyMetrics(
    val stepCount: Int,
    val placementCount: Int,
    val eliminationCount: Int,
    val hardestTechnique: KillerTechnique?,
    val techniqueCounts: Map<KillerTechnique, Int>,
    val cageRequiredSteps: Int,
    val lastCageRequiredFill: Int,
    val score: Int,
)

fun collectKillerMetrics(trace: KillerSolveTrace): KillerDifficultyMetrics {
    val moves = trace.moves
    val cageRequired = trace.steps.filter { it.cageRequired }
    return KillerDifficultyMetrics(
        stepCount = moves.size,
        placementCount = moves.count { it.isPlacement },
        eliminationCount = moves.sumOf { it.eliminations.size },
        hardestTechnique = hardestKillerTechnique(moves.map { it.technique }),
        techniqueCounts = moves.groupingBy { it.technique }.eachCount(),
        cageRequiredSteps = cageRequired.size,
        lastCageRequiredFill = cageRequired.maxOfOrNull { it.filledBefore } ?: 0,
        score = moves.filter { it.technique.tier != Difficulty.EASY }.sumOf { it.score },
    )
}

private val SCORE_CEILINGS: List<Pair<Difficulty, Int>> = listOf(
    Difficulty.EASY to 0,
    Difficulty.MEDIUM to 300,
    Difficulty.HARD to 550,
)

val KILLER_MIN_SCORE: Map<Difficulty, Int> = mapOf(
    Difficulty.EASY to 0,
    Difficulty.MEDIUM to 60,
    Difficulty.HARD to 250,
    Difficulty.EXPERT to 350,
)

val KILLER_MIN_CLASSIC_TAKEOVER: Map<Difficulty, Int> = mapOf(
    Difficulty.EASY to 32,
    Difficulty.MEDIUM to 38,
    Difficulty.HARD to 42,
    Difficulty.EXPERT to 45,
)

fun classifyKillerDifficulty(metrics: KillerDifficultyMetrics): Difficulty {
    val byTechnique = metrics.hardestTechnique?.tier ?: Difficulty.EASY
    val byScore = SCORE_CEILINGS.firstOrNull { metrics.score <= it.second }?.first ?: Difficulty.EXPERT
    return if (byScore.ordinal > byTechnique.ordinal) byScore else byTechnique
}
