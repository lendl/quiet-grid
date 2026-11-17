package com.quietgrid.engine.blockescape

import com.quietgrid.engine.journey.packRawScore
import com.quietgrid.engine.journey.tieredRawScore

private const val BLOCKESCAPE_UNSOLVED_MOVES = 99

fun blockEscapeRawScore(entry: BlockEscapePuzzleEntry): Long {
    val distances = blockEscapeDistances(entry.blocks)
    val fine = if (distances == null) {
        packRawScore(BLOCKESCAPE_UNSOLVED_MOVES to 2)
    } else {
        val start = distances.keyOf(entry.blocks)
        val tracksRepeats = BLOCKESCAPE_REPEAT_MOVES_BY_DIFFICULTY.keys.any { it.key == entry.difficulty }
        val repeats = if (tracksRepeats) distances.repeatMoves(start) ?: 0 else 0
        packRawScore(
            (distances.distances[start] ?: BLOCKESCAPE_UNSOLVED_MOVES) to 2,
            repeats to 2,
            entry.blocks.size to 2,
            distances.distances.size to 6,
        )
    }
    return tieredRawScore(entry.difficulty, fine)
}
