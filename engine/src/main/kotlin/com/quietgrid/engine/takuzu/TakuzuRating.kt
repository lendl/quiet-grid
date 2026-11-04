package com.quietgrid.engine.takuzu

import com.quietgrid.engine.journey.JourneyRatingScale
import com.quietgrid.engine.journey.packRawScore
import com.quietgrid.engine.journey.tieredRawScore

fun takuzuRawScore(entry: TakuzuPuzzleEntry): Long {
    val givens = decodePuzzleBoard(entry.solution, entry.mask, entry.size)
    val metrics = try {
        analyzeTakuzuDifficulty(givens, decodeSolution(entry.solution, entry.size))
    } catch (_: TakuzuDifficultyStalledException) {
        null
    }
    val fine = if (metrics == null) {
        packRawScore(99_999 to 5)
    } else {
        packRawScore(
            computeTakuzuDifficultyScore(entry.size, metrics) to 5,
            metrics.highestTipLevel to 1,
            metrics.openingHighestTipLevel to 1,
            metrics.totalMoves to 3,
        )
    }
    return tieredRawScore(entry.difficulty, fine)
}

fun rateTakuzu(entry: TakuzuPuzzleEntry, scale: JourneyRatingScale): Int = scale.rate(takuzuRawScore(entry))
