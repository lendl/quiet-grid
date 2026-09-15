package com.quietgrid.cli.starbattle

import com.quietgrid.cli.animaldoku.generateAnimalDokuPuzzleForSolution
import com.quietgrid.cli.animaldoku.generateSolutionPermutation
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry

fun generateStarBattleK1PuzzleForTier(
    size: Int,
    sourceAnimalDokuTier: Difficulty,
    targetStarBattleTier: String,
    idPrefix: String,
): StarBattlePuzzleEntry? {
    val solution = generateSolutionPermutation(size) ?: return null
    val animalDokuEntry = generateAnimalDokuPuzzleForSolution(size, solution, sourceAnimalDokuTier, idPrefix) ?: return null
    return StarBattlePuzzleEntry(
        id = animalDokuEntry.id,
        size = animalDokuEntry.size,
        difficulty = targetStarBattleTier,
        k = 1,
        regions = animalDokuEntry.regions,
        solution = animalDokuEntry.solution.map { listOf(it) },
    )
}
