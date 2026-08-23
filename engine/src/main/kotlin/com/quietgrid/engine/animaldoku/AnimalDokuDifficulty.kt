// engine/src/main/kotlin/com/quietgrid/engine/animaldoku/AnimalDokuDifficulty.kt
package com.quietgrid.engine.animaldoku

import com.quietgrid.engine.core.Difficulty

val ANIMALDOKU_SIZES_BY_DIFFICULTY: Map<Difficulty, List<Int>> = mapOf(
    Difficulty.EASY to listOf(4, 5),
    Difficulty.MEDIUM to listOf(4, 5, 6),
    Difficulty.HARD to listOf(7, 8, 9),
    Difficulty.EXPERT to listOf(7, 8, 9),
)

const val ANIMALDOKU_EXPERT_CHAIN_DEPTH_FLOOR = 4
const val ANIMALDOKU_EXPERT_CHAIN_REPEATS_FLOOR = 4

data class AnimalDokuSolveProfile(
    val hardestTechnique: AnimalDokuTechnique,
    val hardestTechniqueRepeats: Int,
    val maxChainDepth: Int,
    val chainRepeats: Int,
)

fun analyzeSolveResult(result: AnimalDokuSolveResult): AnimalDokuSolveProfile {
    val hardestTechnique = result.steps.maxByOrNull { it.technique.ordinal }?.technique ?: AnimalDokuTechnique.SINGLETON
    val chainSteps = result.steps.filter { it.technique == AnimalDokuTechnique.CHAIN }
    val maxChainDepth = chainSteps.maxOfOrNull { it.chainDepth } ?: 0
    val hardestTechniqueRepeats = result.steps.count { it.technique == hardestTechnique }
    return AnimalDokuSolveProfile(hardestTechnique, hardestTechniqueRepeats, maxChainDepth, chainSteps.size)
}

fun isExpertGradeChain(profile: AnimalDokuSolveProfile): Boolean =
    profile.chainRepeats > 0 &&
        (profile.maxChainDepth >= ANIMALDOKU_EXPERT_CHAIN_DEPTH_FLOOR || profile.chainRepeats >= ANIMALDOKU_EXPERT_CHAIN_REPEATS_FLOOR)

fun classifyAnimalDokuDifficulty(size: Int, result: AnimalDokuSolveResult): Difficulty? {
    if (!result.solved) return null
    val profile = analyzeSolveResult(result)
    val totalSteps = result.steps.size

    if (isExpertGradeChain(profile)) return if (size in 7..9) Difficulty.EXPERT else null

    return when {
        size in 4..5 && profile.hardestTechnique == AnimalDokuTechnique.SINGLETON && totalSteps in 4..6 -> Difficulty.EASY
        size in 4..6 && profile.hardestTechnique.ordinal <= AnimalDokuTechnique.CONFINEMENT.ordinal && totalSteps in 6..9 -> Difficulty.MEDIUM
        size in 7..9 && totalSteps in 9..14 -> Difficulty.HARD
        else -> null
    }
}
