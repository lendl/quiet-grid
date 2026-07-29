package com.quietgrid.engine.sudoku

import com.quietgrid.engine.core.Difficulty

data class SudokuDifficultyMetrics(
    val stepCount: Int,
    val placementCount: Int,
    val candidateEliminationCount: Int,
    val advancedStepCount: Int,
    val branchingFactor: Double,
    val branchingScore: Int,
    val openingTechnique: SudokuTechnique?,
    val hardestTechnique: SudokuTechnique?,
    val techniqueCounts: Map<SudokuTechnique, Int>,
    val maxComplexityPerTechnique: Map<SudokuTechnique, Int>,
)

fun collectDifficultyMetrics(moves: List<SudokuCanonicalMove>): SudokuDifficultyMetrics {
    val techniqueCounts = moves.groupingBy { it.technique }.eachCount()
    val maxComplexityPerTechnique = moves.groupBy { it.technique }.mapValues { (_, group) -> group.maxOf { it.complexity } }
    val hardestTechnique = getHardestTechnique(moves.map { it.technique })
    val openingTechnique = moves.firstOrNull()?.technique
    val candidateEliminationCount = moves.filterIsInstance<SudokuCandidateEliminationMove>().sumOf { countMoveTargets(it) }
    val advancedStepCount = moves.count { sudokuTechniqueDifficultyFloor[it.technique] == Difficulty.HARD || sudokuTechniqueDifficultyFloor[it.technique] == Difficulty.EXPERT }
    val branchingFactor = if (moves.isEmpty()) 0.0 else candidateEliminationCount.toDouble() / moves.size
    val branchingScore = moves.sumOf { if (it is SudokuCandidateEliminationMove) maxOf(1, countMoveTargets(it)) else 0 }

    return SudokuDifficultyMetrics(
        stepCount = moves.size,
        placementCount = moves.count { it is SudokuPlacementMove },
        candidateEliminationCount = candidateEliminationCount,
        advancedStepCount = advancedStepCount,
        branchingFactor = branchingFactor,
        branchingScore = branchingScore,
        openingTechnique = openingTechnique,
        hardestTechnique = hardestTechnique,
        techniqueCounts = techniqueCounts,
        maxComplexityPerTechnique = maxComplexityPerTechnique,
    )
}

private data class DifficultyWeights(
    val perStep: Int = 6, val perPlacement: Int = 4, val perCandidateElimination: Int = 3, val perAdvancedStep: Int = 5,
    val techniqueWeights: Map<SudokuTechnique, Int> = mapOf(
        SudokuTechnique.NAKED_SINGLE to 2, SudokuTechnique.HIDDEN_SINGLE to 3, SudokuTechnique.NAKED_PAIR to 6,
        SudokuTechnique.HIDDEN_PAIR to 9, SudokuTechnique.POINTING_PAIR_TRIPLE to 10, SudokuTechnique.BOX_LINE_REDUCTION to 10,
        SudokuTechnique.X_WING to 17, SudokuTechnique.SWORDFISH to 24, SudokuTechnique.XY_WING to 21,
        SudokuTechnique.XYZ_WING to 28, SudokuTechnique.COLORING to 27, SudokuTechnique.CHAINS to 30,
    ),
)

private val WEIGHTS = DifficultyWeights()

data class SudokuDifficultyRails(
    val maxOpeningTechnique: SudokuTechnique?, val maxOverallTechnique: SudokuTechnique?,
    val maxStepCount: Int, val maxAdvancedStepCount: Int, val maxComplexityPerTechnique: Map<SudokuTechnique, Int>,
)

data class SudokuDifficultyProfile(val scoreMin: Int, val scoreMax: Int, val rails: SudokuDifficultyRails)

val SUDOKU_DIFFICULTY_PROFILES: Map<Difficulty, SudokuDifficultyProfile> = mapOf(
    Difficulty.EASY to SudokuDifficultyProfile(0, 360, SudokuDifficultyRails(
        SudokuTechnique.NAKED_PAIR, SudokuTechnique.NAKED_PAIR, 64, 0,
        mapOf(SudokuTechnique.NAKED_SINGLE to 3, SudokuTechnique.HIDDEN_SINGLE to 4, SudokuTechnique.NAKED_PAIR to 5),
    )),
    Difficulty.MEDIUM to SudokuDifficultyProfile(361, 760, SudokuDifficultyRails(
        SudokuTechnique.POINTING_PAIR_TRIPLE, SudokuTechnique.BOX_LINE_REDUCTION, 108, 24,
        mapOf(SudokuTechnique.NAKED_SINGLE to 6, SudokuTechnique.HIDDEN_SINGLE to 7, SudokuTechnique.NAKED_PAIR to 8, SudokuTechnique.HIDDEN_PAIR to 7, SudokuTechnique.POINTING_PAIR_TRIPLE to 5, SudokuTechnique.BOX_LINE_REDUCTION to 5),
    )),
    Difficulty.HARD to SudokuDifficultyProfile(761, 1120, SudokuDifficultyRails(
        SudokuTechnique.NAKED_PAIR, SudokuTechnique.XY_WING, 148, 56,
        mapOf(SudokuTechnique.X_WING to 6, SudokuTechnique.SWORDFISH to 7, SudokuTechnique.XY_WING to 12, SudokuTechnique.COLORING to 8),
    )),
    Difficulty.EXPERT to SudokuDifficultyProfile(1121, Int.MAX_VALUE, SudokuDifficultyRails(
        SudokuTechnique.X_WING, SudokuTechnique.CHAINS, 280, 128, emptyMap(),
    )),
)

fun computeDifficultyScore(metrics: SudokuDifficultyMetrics): Int {
    val techniqueScore = metrics.techniqueCounts.entries.sumOf { (technique, count) -> count * WEIGHTS.techniqueWeights.getValue(technique) }
    return metrics.stepCount * WEIGHTS.perStep + metrics.placementCount * WEIGHTS.perPlacement +
        metrics.candidateEliminationCount * WEIGHTS.perCandidateElimination + metrics.advancedStepCount * WEIGHTS.perAdvancedStep +
        Math.round(metrics.branchingFactor * 10).toInt() + metrics.branchingScore + techniqueScore
}

private fun scoreBucket(score: Int): Difficulty = when {
    score <= SUDOKU_DIFFICULTY_PROFILES.getValue(Difficulty.EASY).scoreMax -> Difficulty.EASY
    score <= SUDOKU_DIFFICULTY_PROFILES.getValue(Difficulty.MEDIUM).scoreMax -> Difficulty.MEDIUM
    score <= SUDOKU_DIFFICULTY_PROFILES.getValue(Difficulty.HARD).scoreMax -> Difficulty.HARD
    else -> Difficulty.EXPERT
}

private fun techniqueBucket(metrics: SudokuDifficultyMetrics): Difficulty =
    metrics.hardestTechnique?.let { sudokuTechniqueDifficultyFloor.getValue(it) } ?: Difficulty.EASY

private fun isWithinCeiling(technique: SudokuTechnique?, ceiling: SudokuTechnique?): Boolean {
    if (technique == null || ceiling == null) return true
    return compareTechniques(technique, ceiling) <= 0
}

private fun passesSafetyRails(difficulty: Difficulty, metrics: SudokuDifficultyMetrics): Boolean {
    val rails = SUDOKU_DIFFICULTY_PROFILES.getValue(difficulty).rails
    if (!isWithinCeiling(metrics.openingTechnique, rails.maxOpeningTechnique)) return false
    if (!isWithinCeiling(metrics.hardestTechnique, rails.maxOverallTechnique)) return false
    if (metrics.stepCount > rails.maxStepCount) return false
    if (metrics.advancedStepCount > rails.maxAdvancedStepCount) return false
    for ((technique, maxComplexity) in rails.maxComplexityPerTechnique) {
        val observed = metrics.maxComplexityPerTechnique[technique]
        if (observed != null && observed > maxComplexity) return false
    }
    return true
}

data class SudokuDifficultyClassification(val difficulty: Difficulty, val score: Int)

fun classifyDifficulty(metrics: SudokuDifficultyMetrics, score: Int = computeDifficultyScore(metrics)): SudokuDifficultyClassification? {
    val bucket = techniqueBucket(metrics)
    var difficulty = if (metrics.hardestTechnique == null) Difficulty.EASY else bucket

    while (!passesSafetyRails(difficulty, metrics)) {
        val next = Difficulty.entries.getOrNull(difficulty.ordinal + 1) ?: return null
        difficulty = next
    }

    val finalDifficulty = if (difficulty.ordinal >= bucket.ordinal) difficulty else bucket
    return SudokuDifficultyClassification(finalDifficulty, score)
}
