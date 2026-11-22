package com.quietgrid.engine.animaldoku

import com.quietgrid.engine.journey.JourneyRatingScale
import com.quietgrid.engine.journey.packRawScore
import com.quietgrid.engine.journey.tieredRawScore

fun animalDokuRawScore(entry: AnimalDokuPuzzleEntry): Long {
    val result = solveAnimalDoku(entry.size, entry.regions)
    val profile = analyzeSolveResult(result)
    val fine = packRawScore(
        profile.chainRepeats to 3,
        profile.hardestTechnique.ordinal to 1,
        entry.size to 2,
        profile.maxChainDepth to 2,
        profile.confinementRepeats to 2,
        profile.stepsBeforeFirstPlacement to 2,
        result.steps.size to 3,
    )
    return tieredRawScore(entry.difficulty, fine)
}

fun rateAnimalDoku(entry: AnimalDokuPuzzleEntry, scale: JourneyRatingScale): Int = scale.rate(animalDokuRawScore(entry))
