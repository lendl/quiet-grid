// cli/src/main/kotlin/com/quietgrid/cli/animaldoku/AnimalDokuHardening.kt
package com.quietgrid.cli.animaldoku

import com.quietgrid.engine.animaldoku.AnimalDokuSolveResult
import com.quietgrid.engine.animaldoku.analyzeSolveResult
import com.quietgrid.engine.animaldoku.classifyAnimalDokuDifficulty
import com.quietgrid.engine.animaldoku.solveAnimalDoku
import com.quietgrid.engine.core.Difficulty

data class HardnessKey(
    val hardestTechniqueOrdinal: Int,
    val hardestTechniqueRepeatCount: Int,
    val maxChainDepth: Int,
) : Comparable<HardnessKey> {
    override fun compareTo(other: HardnessKey): Int {
        hardestTechniqueOrdinal.compareTo(other.hardestTechniqueOrdinal).let { if (it != 0) return it }
        hardestTechniqueRepeatCount.compareTo(other.hardestTechniqueRepeatCount).let { if (it != 0) return it }
        return maxChainDepth.compareTo(other.maxChainDepth)
    }
}

fun hardnessKeyOf(result: AnimalDokuSolveResult): HardnessKey {
    val profile = analyzeSolveResult(result)
    return HardnessKey(profile.hardestTechnique.ordinal, profile.hardestTechniqueRepeats, profile.maxChainDepth)
}

fun isAcceptableHardeningCandidate(
    size: Int,
    candidateResult: AnimalDokuSolveResult,
    candidateKey: HardnessKey,
    bestKey: HardnessKey,
    targetDifficulty: Difficulty,
): Boolean {
    val candidateDifficulty = classifyAnimalDokuDifficulty(size, candidateResult)
    if (candidateDifficulty != null && candidateDifficulty.ordinal > targetDifficulty.ordinal) return false
    return candidateKey >= bestKey
}

fun hardenTowardDifficulty(
    size: Int,
    solution: IntArray,
    initialRegions: List<List<Int>>,
    initialSolveResult: AnimalDokuSolveResult,
    targetDifficulty: Difficulty,
    maxStallMutations: Int = 2000,
): AnimalDokuRepairedPuzzle {
    var bestRegions = initialRegions
    var bestResult = initialSolveResult
    var bestKey = hardnessKeyOf(initialSolveResult)
    var stall = 0

    while (stall < maxStallMutations && classifyAnimalDokuDifficulty(size, bestResult) != targetDifficulty) {
        val mutated = mutateOneBoundaryCell(size, solution, bestRegions)
        if (mutated == null) {
            stall++
            continue
        }
        val candidateResult = solveAnimalDoku(size, mutated)
        if (!candidateResult.solved) {
            stall++
            continue
        }
        val candidateKey = hardnessKeyOf(candidateResult)
        if (!isAcceptableHardeningCandidate(size, candidateResult, candidateKey, bestKey, targetDifficulty)) {
            stall++
            continue
        }
        val improved = candidateKey > bestKey
        bestRegions = mutated
        bestResult = candidateResult
        bestKey = candidateKey
        stall = if (improved) 0 else stall + 1
    }

    return AnimalDokuRepairedPuzzle(bestRegions, bestResult)
}
