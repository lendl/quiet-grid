// engine/src/main/kotlin/com/quietgrid/engine/animaldoku/AnimalDokuDifficulty.kt
package com.quietgrid.engine.animaldoku

import com.quietgrid.engine.core.Difficulty

data class AnimalDokuDifficultyProfile(
    val minSize: Int,
    val maxSize: Int,
    val minTechnique: AnimalDokuTechnique,
    val maxTechnique: AnimalDokuTechnique,
    val minChainDepth: Int,
    val maxChainDepth: Int,
    val maxOpeningTechnique: AnimalDokuTechnique,
    val minHardestTechniqueRepeats: Int,
)

val ANIMALDOKU_DIFFICULTY_PROFILES: Map<Difficulty, AnimalDokuDifficultyProfile> = mapOf(
    Difficulty.EASY to AnimalDokuDifficultyProfile(
        minSize = 4, maxSize = 5,
        minTechnique = AnimalDokuTechnique.SINGLETON, maxTechnique = AnimalDokuTechnique.SINGLETON,
        minChainDepth = 1, maxChainDepth = 0,
        maxOpeningTechnique = AnimalDokuTechnique.SINGLETON, minHardestTechniqueRepeats = 1,
    ),
    Difficulty.MEDIUM to AnimalDokuDifficultyProfile(
        minSize = 4, maxSize = 6,
        minTechnique = AnimalDokuTechnique.SINGLETON, maxTechnique = AnimalDokuTechnique.CONFINEMENT,
        minChainDepth = 1, maxChainDepth = 0,
        maxOpeningTechnique = AnimalDokuTechnique.CONFINEMENT, minHardestTechniqueRepeats = 1,
    ),
    Difficulty.HARD to AnimalDokuDifficultyProfile(
        minSize = 6, maxSize = 8,
        minTechnique = AnimalDokuTechnique.CONFINEMENT, maxTechnique = AnimalDokuTechnique.PAIRING_3,
        minChainDepth = 1, maxChainDepth = 0,
        maxOpeningTechnique = AnimalDokuTechnique.PAIRING_2, minHardestTechniqueRepeats = 4,
    ),
    Difficulty.EXPERT to AnimalDokuDifficultyProfile(
        minSize = 7, maxSize = 8,
        minTechnique = AnimalDokuTechnique.PAIRING_4_PLUS, maxTechnique = AnimalDokuTechnique.CHAIN,
        minChainDepth = 3, maxChainDepth = 10,
        maxOpeningTechnique = AnimalDokuTechnique.PAIRING_3, minHardestTechniqueRepeats = 2,
    ),
)

val ANIMALDOKU_SIZES_BY_DIFFICULTY: Map<Difficulty, List<Int>> = mapOf(
    Difficulty.EASY to listOf(4, 5),
    Difficulty.MEDIUM to listOf(4, 5, 6),
    Difficulty.HARD to listOf(6, 7, 8),
    Difficulty.EXPERT to listOf(7, 8),
)

private fun fitsProfile(
    hardestTechnique: AnimalDokuTechnique,
    maxChainDepth: Int,
    openingTechnique: AnimalDokuTechnique,
    hardestTechniqueRepeats: Int,
    profile: AnimalDokuDifficultyProfile,
): Boolean {
    if (openingTechnique.ordinal > profile.maxOpeningTechnique.ordinal) return false
    if (hardestTechniqueRepeats < profile.minHardestTechniqueRepeats) return false
    if (hardestTechnique == AnimalDokuTechnique.CHAIN) return maxChainDepth in profile.minChainDepth..profile.maxChainDepth
    return hardestTechnique.ordinal >= profile.minTechnique.ordinal && hardestTechnique.ordinal <= profile.maxTechnique.ordinal
}

data class AnimalDokuSolveProfile(
    val hardestTechnique: AnimalDokuTechnique,
    val hardestTechniqueRepeats: Int,
    val maxChainDepth: Int,
    val openingTechnique: AnimalDokuTechnique,
)

fun analyzeSolveResult(result: AnimalDokuSolveResult): AnimalDokuSolveProfile {
    val hardestTechnique = result.steps.maxByOrNull { it.technique.ordinal }?.technique ?: AnimalDokuTechnique.SINGLETON
    val maxChainDepth = result.steps.filter { it.technique == AnimalDokuTechnique.CHAIN }.maxOfOrNull { it.chainDepth } ?: 0
    val openingTechnique = result.steps.firstOrNull()?.technique ?: AnimalDokuTechnique.SINGLETON
    val hardestTechniqueRepeats = result.steps.count { it.technique == hardestTechnique }
    return AnimalDokuSolveProfile(hardestTechnique, hardestTechniqueRepeats, maxChainDepth, openingTechnique)
}

fun classifyAnimalDokuDifficulty(size: Int, result: AnimalDokuSolveResult): Difficulty? {
    if (!result.solved) return null
    val profile = analyzeSolveResult(result)

    for (difficulty in Difficulty.entries) {
        val tierProfile = ANIMALDOKU_DIFFICULTY_PROFILES.getValue(difficulty)
        if (size < tierProfile.minSize || size > tierProfile.maxSize) continue
        if (fitsProfile(profile.hardestTechnique, profile.maxChainDepth, profile.openingTechnique, profile.hardestTechniqueRepeats, tierProfile)) return difficulty
    }
    return null
}
