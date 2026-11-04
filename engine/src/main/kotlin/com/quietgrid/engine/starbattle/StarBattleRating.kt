package com.quietgrid.engine.starbattle

import com.quietgrid.engine.journey.JourneyRatingScale
import com.quietgrid.engine.journey.packRawScore
import com.quietgrid.engine.journey.tieredRawScore

fun starBattleRawScore(entry: StarBattlePuzzleEntry): Long {
    val result = solveStarBattle(entry.size, entry.k, entry.regions)
    val profile = analyzeStarBattleSolveResult(result)
    val fine = packRawScore(
        entry.k to 1,
        profile.chainRepeats to 2,
        profile.hardestTechnique.ordinal to 1,
        entry.size to 2,
        profile.maxChainDepth to 2,
        profile.confinementRepeats to 2,
        profile.stepsBeforeFirstPlacement to 2,
        result.steps.size to 3,
    )
    return tieredRawScore(entry.difficulty, fine)
}

fun rateStarBattle(entry: StarBattlePuzzleEntry, scale: JourneyRatingScale): Int = scale.rate(starBattleRawScore(entry))
