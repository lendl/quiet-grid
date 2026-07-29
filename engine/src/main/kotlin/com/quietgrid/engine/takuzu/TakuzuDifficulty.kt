package com.quietgrid.engine.takuzu

import com.quietgrid.engine.core.Difficulty

data class TakuzuTipUsageCounts(
    var findPairs: Int = 0,
    var avoidTrios: Int = 0,
    var completeLines: Int = 0,
    var eliminateFilledLines: Int = 0,
    var eliminateImpossibleCombinations: Int = 0,
)

data class TakuzuDifficultyMetrics(
    val givenCount: Int,
    var sparseMoveCount: Int = 0,
    var tipSequencePressure: Int = 0,
    var highestTipLevel: Int = 0,
    var openingHighestTipLevel: Int = 0,
    var totalMoves: Int = 0,
    var openingMoves: Int = 0,
    val tipUsageCounts: TakuzuTipUsageCounts = TakuzuTipUsageCounts(),
    val openingTipUsageCounts: TakuzuTipUsageCounts = TakuzuTipUsageCounts(),
    var impossibleCombinationMaxLineCompletions: Int = 0,
)

class TakuzuDifficultyStalledException(board: TakuzuGrid) :
    Exception("Could not fully analyze takuzu puzzle with current logical tips.\n${formatBoard(board)}")

private fun formatBoard(board: TakuzuGrid): String =
    board.joinToString("\n") { row -> row.joinToString(" ") { it?.toString() ?: "." } }

private val TIP_LEVELS: Map<TakuzuTechnique, Int> = mapOf(
    TakuzuTechnique.FIND_PAIRS to 1,
    TakuzuTechnique.AVOID_TRIOS to 1,
    TakuzuTechnique.COMPLETE_LINES to 2,
    TakuzuTechnique.ELIMINATE_FILLED_LINES to 3,
    TakuzuTechnique.ELIMINATE_IMPOSSIBLE_COMBINATIONS to 4,
)

fun analyzeTakuzuDifficulty(givens: TakuzuGrid, solution: TakuzuGrid): TakuzuDifficultyMetrics {
    val size = givens.size
    val board: MutableList<MutableList<TakuzuCellValue>> = givens.map { it.toMutableList() }.toMutableList()
    val metrics = TakuzuDifficultyMetrics(givenCount = givens.sumOf { row -> row.count { it != null } })

    var previousLevel: Int? = null
    var safetyCounter = 0
    val moveHistory = mutableListOf<TakuzuTechnique>()

    fun isSolved() = board.all { row -> row.all { it != null } }

    while (!isSolved() && safetyCounter < size * size * 4) {
        safetyCounter += 1
        val readOnlyBoard = board.map { it.toList() }
        val move = findPairsMove(readOnlyBoard)
            ?: findAvoidTriosMove(readOnlyBoard)
            ?: findCompleteLinesMove(readOnlyBoard)
            ?: findEliminateFilledLinesMove(readOnlyBoard)
            ?: findImpossibleCombinationMove(readOnlyBoard)
            ?: throw TakuzuDifficultyStalledException(readOnlyBoard)

        check(solution[move.row][move.col] == move.value) { "Analyzer produced an incorrect move at ${move.row},${move.col}." }

        val row = board[move.row]
        val col = getColumn(board.map { it.toList() }, move.col)
        val rowEmpties = countEmpties(row)
        val colEmpties = countEmpties(col)
        val level = TIP_LEVELS.getValue(move.technique)

        when (move.technique) {
            TakuzuTechnique.FIND_PAIRS -> metrics.tipUsageCounts.findPairs += 1
            TakuzuTechnique.AVOID_TRIOS -> metrics.tipUsageCounts.avoidTrios += 1
            TakuzuTechnique.COMPLETE_LINES -> metrics.tipUsageCounts.completeLines += 1
            TakuzuTechnique.ELIMINATE_FILLED_LINES -> metrics.tipUsageCounts.eliminateFilledLines += 1
            TakuzuTechnique.ELIMINATE_IMPOSSIBLE_COMBINATIONS -> metrics.tipUsageCounts.eliminateImpossibleCombinations += 1
        }
        metrics.highestTipLevel = maxOf(metrics.highestTipLevel, level)
        metrics.totalMoves += 1
        if (maxOf(rowEmpties, colEmpties) >= size / 2) metrics.sparseMoveCount += 1
        if (previousLevel != null && previousLevel!! >= 3 && level >= previousLevel!!) metrics.tipSequencePressure += 1

        board[move.row][move.col] = move.value
        moveHistory.add(move.technique)
        previousLevel = level
    }

    if (!isSolved() || !hasUniqueLines(board.map { row -> row.map { it!! } })) {
        error("Takuzu difficulty analysis did not finish with a solved, valid board.")
    }

    val openingCount = if (moveHistory.isEmpty()) 0 else minOf(moveHistory.size, maxOf(10, kotlin.math.ceil(moveHistory.size * 0.3).toInt()))
    metrics.openingMoves = openingCount
    for (technique in moveHistory.take(openingCount)) {
        when (technique) {
            TakuzuTechnique.FIND_PAIRS -> metrics.openingTipUsageCounts.findPairs += 1
            TakuzuTechnique.AVOID_TRIOS -> metrics.openingTipUsageCounts.avoidTrios += 1
            TakuzuTechnique.COMPLETE_LINES -> metrics.openingTipUsageCounts.completeLines += 1
            TakuzuTechnique.ELIMINATE_FILLED_LINES -> metrics.openingTipUsageCounts.eliminateFilledLines += 1
            TakuzuTechnique.ELIMINATE_IMPOSSIBLE_COMBINATIONS -> metrics.openingTipUsageCounts.eliminateImpossibleCombinations += 1
        }
        metrics.openingHighestTipLevel = maxOf(metrics.openingHighestTipLevel, TIP_LEVELS.getValue(technique))
    }

    return metrics
}

private data class ScoreWeights(
    val sizeBase: Map<Int, Int> = mapOf(6 to 120, 8 to 340, 10 to 620),
    val revealGap: Int = 18,
    val sparseMove: Int = 10,
    val tipSequencePressure: Int = 22,
    val highestTipLevel: Int = 40,
    val openingHighestTipLevel: Int = 56,
    val impossibleCombinationPressure: Int = 18,
    val tipUsageFindPairs: Int = 4,
    val tipUsageAvoidTrios: Int = 6,
    val tipUsageCompleteLines: Int = 9,
    val tipUsageEliminateFilledLines: Int = 16,
    val tipUsageEliminateImpossibleCombinations: Int = 24,
)

private val SCORE_WEIGHTS = ScoreWeights()

private fun revealBounds(size: Int): Pair<Int, Int> {
    val total = size * size
    return (total * 15 / 100) to (total * 25 / 100)
}

data class TakuzuDifficultyRails(
    val maxOpeningTipLevel: Int,
    val maxOverallTipLevel: Int,
    val maxSparseMoveCount: Int,
    val allowImpossibleCombinations: Boolean,
    val maxImpossibleCombinationLineCompletions: Int,
    val minGivenCount: Int? = null,
    val maxGivenCount: Int? = null,
)

data class TakuzuDifficultyBucket(
    val size: Int,
    val difficulty: Difficulty,
    val minScore: Int,
    val maxScore: Int,
    val rails: TakuzuDifficultyRails,
)

val TAKUZU_SUPPORTED_SIZES = listOf(6, 8, 10)

val TAKUZU_SUPPORTED_BUCKETS: List<TakuzuDifficultyBucket> = listOf(
    TakuzuDifficultyBucket(6, Difficulty.EASY, 0, 999, TakuzuDifficultyRails(2, 4, 24, true, 2, 10, 14)),
    TakuzuDifficultyBucket(6, Difficulty.MEDIUM, 1000, Int.MAX_VALUE, TakuzuDifficultyRails(3, 4, 30, true, 4, 6, 10)),
    TakuzuDifficultyBucket(8, Difficulty.EASY, 0, 1199, TakuzuDifficultyRails(2, 4, 36, true, 2)),
    TakuzuDifficultyBucket(8, Difficulty.MEDIUM, 1200, 1700, TakuzuDifficultyRails(4, 4, 40, true, 4)),
    TakuzuDifficultyBucket(8, Difficulty.HARD, 1450, Int.MAX_VALUE, TakuzuDifficultyRails(4, 4, Int.MAX_VALUE, true, 8)),
    TakuzuDifficultyBucket(10, Difficulty.MEDIUM, 0, 2350, TakuzuDifficultyRails(2, 4, 56, true, 4)),
    TakuzuDifficultyBucket(10, Difficulty.HARD, 1900, 2750, TakuzuDifficultyRails(4, 4, Int.MAX_VALUE, true, 8)),
    TakuzuDifficultyBucket(10, Difficulty.EXPERT, 2500, Int.MAX_VALUE, TakuzuDifficultyRails(4, 4, Int.MAX_VALUE, true, 15)),
)

fun getTakuzuTargetRevealBounds(size: Int, difficulty: Difficulty): Pair<Int, Int> {
    val bucket = TAKUZU_SUPPORTED_BUCKETS.find { it.size == size && it.difficulty == difficulty }
    val fallback = revealBounds(size)
    return (bucket?.rails?.minGivenCount ?: fallback.first) to (bucket?.rails?.maxGivenCount ?: fallback.second)
}

fun computeTakuzuDifficultyScore(size: Int, metrics: TakuzuDifficultyMetrics): Int {
    val revealBounds = revealBounds(size)
    val sizeContribution = SCORE_WEIGHTS.sizeBase[size] ?: 0
    val tipContribution = metrics.tipUsageCounts.findPairs * SCORE_WEIGHTS.tipUsageFindPairs +
        metrics.tipUsageCounts.avoidTrios * SCORE_WEIGHTS.tipUsageAvoidTrios +
        metrics.tipUsageCounts.completeLines * SCORE_WEIGHTS.tipUsageCompleteLines +
        metrics.tipUsageCounts.eliminateFilledLines * SCORE_WEIGHTS.tipUsageEliminateFilledLines +
        metrics.tipUsageCounts.eliminateImpossibleCombinations * SCORE_WEIGHTS.tipUsageEliminateImpossibleCombinations
    val revealContribution = maxOf(0, revealBounds.second - metrics.givenCount) * SCORE_WEIGHTS.revealGap

    return sizeContribution + revealContribution + tipContribution +
        metrics.sparseMoveCount * SCORE_WEIGHTS.sparseMove +
        metrics.tipSequencePressure * SCORE_WEIGHTS.tipSequencePressure +
        metrics.highestTipLevel * SCORE_WEIGHTS.highestTipLevel +
        metrics.openingHighestTipLevel * SCORE_WEIGHTS.openingHighestTipLevel +
        metrics.impossibleCombinationMaxLineCompletions * SCORE_WEIGHTS.impossibleCombinationPressure
}

private fun highestTechniqueLevel(metrics: TakuzuDifficultyMetrics): Int = when {
    metrics.tipUsageCounts.eliminateImpossibleCombinations > 0 -> 5
    metrics.tipUsageCounts.eliminateFilledLines > 0 -> 4
    metrics.tipUsageCounts.completeLines > 0 -> 3
    metrics.tipUsageCounts.avoidTrios > 0 -> 2
    else -> 1
}

private fun difficultyFromTechniqueLevel(level: Int): Difficulty = when {
    level >= 5 -> Difficulty.EXPERT
    level >= 4 -> Difficulty.HARD
    level >= 3 -> Difficulty.MEDIUM
    else -> Difficulty.EASY
}

private fun difficultyFromScore(size: Int, score: Int): Difficulty? =
    TAKUZU_SUPPORTED_BUCKETS.find { it.size == size && score >= it.minScore && score <= it.maxScore }?.difficulty

private fun applyGridSizeConstraints(size: Int, difficulty: Difficulty, highestTechnique: Int): Difficulty = when {
    size == 6 && difficulty > Difficulty.MEDIUM -> Difficulty.MEDIUM
    size == 8 && difficulty == Difficulty.EXPERT && highestTechnique < 5 -> Difficulty.HARD
    size == 10 && difficulty == Difficulty.EASY -> Difficulty.MEDIUM
    else -> difficulty
}

private fun passesSafetyRails(metrics: TakuzuDifficultyMetrics, bucket: TakuzuDifficultyBucket): Boolean {
    val rails = bucket.rails
    if (metrics.openingHighestTipLevel > rails.maxOpeningTipLevel) return false
    if (metrics.highestTipLevel > rails.maxOverallTipLevel) return false
    if (metrics.sparseMoveCount > rails.maxSparseMoveCount) return false
    if (!rails.allowImpossibleCombinations && metrics.tipUsageCounts.eliminateImpossibleCombinations > 0) return false
    if (metrics.impossibleCombinationMaxLineCompletions > rails.maxImpossibleCombinationLineCompletions) return false
    return true
}

/**
 * Checks whether [metrics] passes the safety rails for [difficulty]'s bucket at [size],
 * independent of whichever bucket the metrics would otherwise be classified into.
 * Mirrors the RN reference's standalone `passesDifficultyRails`.
 */
fun passesTakuzuDifficultyRails(size: Int, difficulty: Difficulty, metrics: TakuzuDifficultyMetrics): Boolean {
    val bucket = TAKUZU_SUPPORTED_BUCKETS.find { it.size == size && it.difficulty == difficulty } ?: return false
    return passesSafetyRails(metrics, bucket)
}

fun classifyTakuzuDifficulty(size: Int, metrics: TakuzuDifficultyMetrics, score: Int): Difficulty? {
    val highestTechnique = highestTechniqueLevel(metrics)
    val techniqueDifficulty = difficultyFromTechniqueLevel(highestTechnique)
    val scoreBucket = difficultyFromScore(size, score) ?: return null
    val combined = maxOf(techniqueDifficulty, scoreBucket)
    val constrained = applyGridSizeConstraints(size, combined, highestTechnique)
    val matchingBucket = TAKUZU_SUPPORTED_BUCKETS.find { it.size == size && it.difficulty == constrained } ?: return null
    return if (passesSafetyRails(metrics, matchingBucket)) matchingBucket.difficulty else null
}
