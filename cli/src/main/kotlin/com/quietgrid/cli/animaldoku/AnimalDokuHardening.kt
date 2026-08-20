// cli/src/main/kotlin/com/quietgrid/cli/animaldoku/AnimalDokuHardening.kt
package com.quietgrid.cli.animaldoku

import com.quietgrid.engine.animaldoku.AnimalDokuSolveResult
import com.quietgrid.engine.animaldoku.AnimalDokuTechnique
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

fun maxHardestTechniqueOrdinalFor(targetDifficulty: Difficulty): Int? = when (targetDifficulty) {
    Difficulty.HARD -> AnimalDokuTechnique.PAIRING_3.ordinal
    else -> null
}

fun isAcceptableHardeningCandidate(candidateKey: HardnessKey, bestKey: HardnessKey, maxHardestTechniqueOrdinal: Int?): Boolean {
    if (maxHardestTechniqueOrdinal != null && candidateKey.hardestTechniqueOrdinal > maxHardestTechniqueOrdinal) return false
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
    val maxHardestTechniqueOrdinal = maxHardestTechniqueOrdinalFor(targetDifficulty)
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
        if (!isAcceptableHardeningCandidate(candidateKey, bestKey, maxHardestTechniqueOrdinal)) {
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
