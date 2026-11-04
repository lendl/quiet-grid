package com.quietgrid.engine.nonogram

import com.quietgrid.engine.journey.JourneyRatingScale
import com.quietgrid.engine.journey.packRawScore
import com.quietgrid.engine.journey.tieredRawScore

fun analyzeNonogramEntry(entry: NonogramPuzzleEntry): NonogramDifficultyMetrics? {
    val rowClues = entry.solution.map(::buildNonogramClues)
    val colClues = (0 until entry.cols).map { col -> buildNonogramClues(entry.solution.map { it[col] }) }
    return analyzeNonogramDifficulty(rowClues, colClues, entry.solution)
}

fun nonogramRawScore(entry: NonogramPuzzleEntry): Long {
    val metrics = analyzeNonogramEntry(entry)
    val fine = if (metrics == null) {
        packRawScore(9_999 to 4)
    } else {
        packRawScore(
            computeNonogramScore(metrics) to 4,
            metrics.hardestLineTier.ordinal to 1,
            metrics.maxChainDepth to 2,
            (entry.rows * entry.cols) to 3,
            metrics.steps to 3,
        )
    }
    return tieredRawScore(entry.difficulty, fine)
}

fun rateNonogram(entry: NonogramPuzzleEntry, scale: JourneyRatingScale): Int = scale.rate(nonogramRawScore(entry))
