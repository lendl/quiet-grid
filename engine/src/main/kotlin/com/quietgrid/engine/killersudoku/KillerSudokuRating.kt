package com.quietgrid.engine.killersudoku

import com.quietgrid.engine.journey.JourneyRatingScale
import com.quietgrid.engine.journey.packRawScore
import com.quietgrid.engine.journey.tieredRawScore

fun killerSudokuRawScore(entry: KillerSudokuPuzzleEntry): Long {
    val metrics = collectKillerMetrics(traceKillerSolve(KillerCageLayout(entry.cages)))
    val fine = packRawScore(
        metrics.score to 4,
        ((metrics.hardestTechnique?.ordinal ?: -1) + 1) to 2,
        metrics.eliminationCount to 4,
        metrics.stepCount to 3,
    )
    return tieredRawScore(entry.difficulty, fine)
}

fun rateKillerSudoku(entry: KillerSudokuPuzzleEntry, scale: JourneyRatingScale): Int = scale.rate(killerSudokuRawScore(entry))
