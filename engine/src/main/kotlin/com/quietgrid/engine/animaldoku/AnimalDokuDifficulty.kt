// engine/src/main/kotlin/com/quietgrid/engine/animaldoku/AnimalDokuDifficulty.kt
package com.quietgrid.engine.animaldoku

import com.quietgrid.engine.core.Difficulty

val ANIMALDOKU_SIZES_BY_DIFFICULTY: Map<Difficulty, List<Int>> = mapOf(
    Difficulty.EASY to listOf(4, 5),
    Difficulty.MEDIUM to listOf(4, 5, 6),
    Difficulty.HARD to listOf(8, 9),
    Difficulty.EXPERT to listOf(9, 10),
)

const val ANIMALDOKU_HARD_MIN_CHAINS = 3
const val ANIMALDOKU_HARD_MIN_STEPS_BEFORE_FIRST_PLACEMENT = 3
const val ANIMALDOKU_HARD_MIN_REGION_SIZE = 3

const val ANIMALDOKU_EXPERT_MIN_CHAINS = 10
const val ANIMALDOKU_EXPERT_MIN_CHAIN_DEPTH = 6
const val ANIMALDOKU_EXPERT_MIN_STEPS_BEFORE_FIRST_PLACEMENT = 8
const val ANIMALDOKU_EXPERT_MIN_REGION_SIZE = 4
const val ANIMALDOKU_EXPERT_MIN_LARGEST_REGION_SHARE = 0.25

data class AnimalDokuSolveProfile(
    val hardestTechnique: AnimalDokuTechnique,
    val hardestTechniqueRepeats: Int,
    val maxChainDepth: Int,
    val chainRepeats: Int,
    val confinementRepeats: Int,
    val stepsBeforeFirstPlacement: Int,
)

fun analyzeSolveResult(result: AnimalDokuSolveResult): AnimalDokuSolveProfile {
    val hardestTechnique = result.steps.maxByOrNull { it.technique.ordinal }?.technique ?: AnimalDokuTechnique.SINGLETON
    val chainSteps = result.steps.filter { it.technique == AnimalDokuTechnique.CHAIN }
    val maxChainDepth = chainSteps.maxOfOrNull { it.chainDepth } ?: 0
    val hardestTechniqueRepeats = result.steps.count { it.technique == hardestTechnique }
    val confinementRepeats = result.steps.count { it.technique == AnimalDokuTechnique.CONFINEMENT }
    val firstPlacement = result.steps.indexOfFirst { it.technique == AnimalDokuTechnique.SINGLETON }
    val stepsBeforeFirstPlacement = if (firstPlacement == -1) result.steps.size else firstPlacement
    return AnimalDokuSolveProfile(
        hardestTechnique,
        hardestTechniqueRepeats,
        maxChainDepth,
        chainSteps.size,
        confinementRepeats,
        stepsBeforeFirstPlacement,
    )
}

fun isExpertGradeChain(profile: AnimalDokuSolveProfile): Boolean =
    profile.chainRepeats >= ANIMALDOKU_EXPERT_MIN_CHAINS &&
        profile.maxChainDepth >= ANIMALDOKU_EXPERT_MIN_CHAIN_DEPTH &&
        profile.stepsBeforeFirstPlacement >= ANIMALDOKU_EXPERT_MIN_STEPS_BEFORE_FIRST_PLACEMENT

fun isHardGradeChain(profile: AnimalDokuSolveProfile): Boolean =
    profile.chainRepeats >= ANIMALDOKU_HARD_MIN_CHAINS &&
        profile.stepsBeforeFirstPlacement >= ANIMALDOKU_HARD_MIN_STEPS_BEFORE_FIRST_PLACEMENT

private fun Difficulty.allowsSize(size: Int): Boolean = size in ANIMALDOKU_SIZES_BY_DIFFICULTY.getValue(this)

fun classifyAnimalDokuDifficulty(size: Int, result: AnimalDokuSolveResult): Difficulty? {
    if (!result.solved) return null
    val profile = analyzeSolveResult(result)
    val totalSteps = result.steps.size

    if (isExpertGradeChain(profile)) return if (Difficulty.EXPERT.allowsSize(size)) Difficulty.EXPERT else null
    if (isHardGradeChain(profile)) return if (Difficulty.HARD.allowsSize(size)) Difficulty.HARD else null

    return when {
        size in 4..5 && profile.hardestTechnique.ordinal <= AnimalDokuTechnique.STRUCTURAL_CONFINEMENT.ordinal && totalSteps in 4..6 -> Difficulty.EASY
        size in 4..6 && profile.hardestTechnique.ordinal <= AnimalDokuTechnique.CONFINEMENT.ordinal && totalSteps in 6..9 -> Difficulty.MEDIUM
        else -> null
    }
}

fun animalDokuRegionSizes(size: Int, regions: List<List<Int>>): IntArray {
    val sizes = IntArray(size)
    for (row in regions) for (region in row) sizes[region]++
    return sizes
}

fun meetsAnimalDokuRegionShape(difficulty: Difficulty, size: Int, regions: List<List<Int>>): Boolean {
    val sizes = animalDokuRegionSizes(size, regions)
    return when (difficulty) {
        Difficulty.HARD -> sizes.min() >= ANIMALDOKU_HARD_MIN_REGION_SIZE
        Difficulty.EXPERT -> sizes.min() >= ANIMALDOKU_EXPERT_MIN_REGION_SIZE &&
            sizes.max() >= ANIMALDOKU_EXPERT_MIN_LARGEST_REGION_SHARE * size * size
        else -> true
    }
}

fun minAnimalDokuRegionSize(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.HARD -> ANIMALDOKU_HARD_MIN_REGION_SIZE
    Difficulty.EXPERT -> ANIMALDOKU_EXPERT_MIN_REGION_SIZE
    else -> 1
}

fun classifyAnimalDokuPuzzle(size: Int, regions: List<List<Int>>, result: AnimalDokuSolveResult): Difficulty? {
    val difficulty = classifyAnimalDokuDifficulty(size, result) ?: return null
    return if (meetsAnimalDokuRegionShape(difficulty, size, regions)) difficulty else null
}
