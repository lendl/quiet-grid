// cli/src/main/kotlin/com/quietgrid/cli/animaldoku/AnimalDokuHardening.kt
package com.quietgrid.cli.animaldoku

import com.quietgrid.engine.animaldoku.AnimalDokuSolveResult
import com.quietgrid.engine.animaldoku.AnimalDokuTechnique
import com.quietgrid.engine.animaldoku.analyzeSolveResult
import com.quietgrid.engine.animaldoku.classifyAnimalDokuDifficulty
import com.quietgrid.engine.animaldoku.solveAnimalDoku
import com.quietgrid.engine.core.Difficulty

/**
 * A comparable summary of how hard a solve trace is, used to hill-climb a region layout toward a
 * target difficulty tier without needing the full classifier on every comparison. Compares
 * lexicographically: hardest technique reached first, then how many times it recurred, then (for a
 * chain-hardest trace) how deep the chain went.
 */
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

/** Reduces a solved trace to its [HardnessKey], reusing the engine's own solve-trace scan. */
fun hardnessKeyOf(result: AnimalDokuSolveResult): HardnessKey {
    val profile = analyzeSolveResult(result)
    return HardnessKey(profile.hardestTechnique.ordinal, profile.hardestTechniqueRepeats, profile.maxChainDepth)
}

/**
 * The highest technique ordinal a hardening search for [targetDifficulty] is allowed to reach, or
 * null for no ceiling. HARD must never be allowed to climb into chain territory -- chain stays
 * EXPERT-exclusive, which is what keeps the two tiers meaningfully separated -- so its ceiling is
 * pinned at PAIRING_3. EXPERT's climb is open-ended: reaching (or going deeper into) CHAIN is
 * always progress, so it has no ceiling.
 */
fun maxHardestTechniqueOrdinalFor(targetDifficulty: Difficulty): Int? = when (targetDifficulty) {
    Difficulty.HARD -> AnimalDokuTechnique.PAIRING_3.ordinal
    else -> null
}

/**
 * Whether a hardening mutation's [candidateKey] should replace [bestKey]. Two gates: (1) if
 * [maxHardestTechniqueOrdinal] is set, a candidate that exceeds it is rejected outright regardless
 * of how favorably it would otherwise compare -- this is what stops HARD's search from wandering
 * into a technique territory that belongs to a different tier. (2) Among candidates that respect
 * the ceiling, only accept a non-regression: [candidateKey] must be `>=` [bestKey]. The search can
 * plateau or improve; it can never accept a mutation that makes the puzzle easier than what's
 * already been found.
 */
fun isAcceptableHardeningCandidate(candidateKey: HardnessKey, bestKey: HardnessKey, maxHardestTechniqueOrdinal: Int?): Boolean {
    if (maxHardestTechniqueOrdinal != null && candidateKey.hardestTechniqueOrdinal > maxHardestTechniqueOrdinal) return false
    return candidateKey >= bestKey
}

/**
 * Hill-climbs an already-uniquely-solvable region layout toward [targetDifficulty], reusing the
 * same boundary-cell mutation [mutateOneBoundaryCell] already uses for repair. Stops the moment the
 * layout actually classifies as [targetDifficulty], or after [maxStallMutations] mutations in a row
 * fail to make genuine progress (whichever comes first). Never returns anything worse than
 * [initialSolveResult] -- the worst case is exactly what was passed in.
 *
 * The stall counter only resets on a *strict* improvement (`candidateKey > bestKey`), not merely an
 * accepted one. [isAcceptableHardeningCandidate] also accepts equal-hardness ("plateau") mutations
 * -- that's deliberate, since wandering sideways can help the search escape a local optimum -- but
 * if the stall counter reset on every accepted plateau move too, an unlucky run of same-hardness
 * mutations could reset it indefinitely and the search would never actually terminate within
 * [maxStallMutations], defeating the whole point of the budget.
 */
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
