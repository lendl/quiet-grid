package com.quietgrid.cli.starbattle

import com.quietgrid.engine.starbattle.StarBattleSolveResult
import com.quietgrid.engine.starbattle.analyzeStarBattleSolveResult
import com.quietgrid.engine.starbattle.classifyStarBattleK2Grade
import com.quietgrid.engine.starbattle.solveStarBattle

data class StarBattleHardnessKey(val maxChainDepth: Int, val chainRepeats: Int) : Comparable<StarBattleHardnessKey> {
    override fun compareTo(other: StarBattleHardnessKey): Int {
        maxChainDepth.compareTo(other.maxChainDepth).let { if (it != 0) return it }
        return chainRepeats.compareTo(other.chainRepeats)
    }
}

fun starBattleHardnessKeyOf(result: StarBattleSolveResult): StarBattleHardnessKey {
    val profile = analyzeStarBattleSolveResult(result)
    return StarBattleHardnessKey(profile.maxChainDepth, profile.chainRepeats)
}

private val K2_GRADE_ORDER = listOf("medium", "hard", "expert")

internal fun starBattleGradeRank(grade: String?): Int = K2_GRADE_ORDER.indexOf(grade)

fun isAcceptableSofteningCandidate(
    candidateResult: StarBattleSolveResult,
    candidateKey: StarBattleHardnessKey,
    bestKey: StarBattleHardnessKey,
    targetGrade: String,
): Boolean {
    val candidateGrade = classifyStarBattleK2Grade(candidateResult)
    if (starBattleGradeRank(candidateGrade) < starBattleGradeRank(targetGrade)) return false
    return candidateKey <= bestKey
}

fun softenStarBattleTowardGrade(
    size: Int,
    k: Int,
    solution: List<List<Int>>,
    initialRegions: List<List<Int>>,
    initialSolveResult: StarBattleSolveResult,
    targetGrade: String,
    maxStallMutations: Int = 2000,
): StarBattleRepairedPuzzle {
    if (starBattleGradeRank(classifyStarBattleK2Grade(initialSolveResult)) < starBattleGradeRank(targetGrade)) {
        return StarBattleRepairedPuzzle(initialRegions, initialSolveResult)
    }

    var bestRegions = initialRegions
    var bestResult = initialSolveResult
    var bestKey = starBattleHardnessKeyOf(initialSolveResult)
    var stall = 0

    while (stall < maxStallMutations && classifyStarBattleK2Grade(bestResult) != targetGrade) {
        val mutated = mutateOneStarBattleBoundaryCell(size, solution, bestRegions)
        if (mutated == null) {
            stall++
            continue
        }
        val candidateResult = solveStarBattle(size, k, mutated)
        if (!candidateResult.solved) {
            stall++
            continue
        }
        val candidateKey = starBattleHardnessKeyOf(candidateResult)
        if (!isAcceptableSofteningCandidate(candidateResult, candidateKey, bestKey, targetGrade)) {
            stall++
            continue
        }
        val improved = candidateKey < bestKey
        bestRegions = mutated
        bestResult = candidateResult
        bestKey = candidateKey
        stall = if (improved) 0 else stall + 1
    }

    return StarBattleRepairedPuzzle(bestRegions, bestResult)
}
