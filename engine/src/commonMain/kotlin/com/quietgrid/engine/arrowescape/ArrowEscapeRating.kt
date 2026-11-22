package com.quietgrid.engine.arrowescape

import com.quietgrid.engine.journey.JourneyRatingScale
import com.quietgrid.engine.journey.packRawScore
import com.quietgrid.engine.journey.tieredRawScore
import kotlin.math.floor

fun arrowEscapeRawScore(entry: ArrowEscapePuzzleEntry): Long {
    val profile = profileArrowEscapePuzzle(entry.pieces.map { it.toPiece() }, entry.rows, entry.cols)
    val fine = packRawScore(
        floor((1.0 - profile.freeShare) * 20).toInt() to 2,
        profile.pieceCount to 4,
        profile.rounds to 3,
    )
    return tieredRawScore(entry.difficulty, fine)
}

fun rateArrowEscape(entry: ArrowEscapePuzzleEntry, scale: JourneyRatingScale): Int = scale.rate(arrowEscapeRawScore(entry))
