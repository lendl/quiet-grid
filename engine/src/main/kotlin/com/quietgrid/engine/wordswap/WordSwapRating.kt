package com.quietgrid.engine.wordswap

import com.quietgrid.engine.journey.packRawScore
import com.quietgrid.engine.journey.tieredRawScore

private const val WORDSWAP_UNSOLVED_SCORE = 99

fun wordSwapRawScore(entry: WordSwapPuzzleEntry): Long {
    val result = solveWordSwap(entry)
    val fine = if (!result.solved) {
        packRawScore(WORDSWAP_UNSOLVED_SCORE to 2)
    } else {
        val counts = analyzeWordSwapSolveResult(result).counts
        packRawScore(
            entry.minSwaps to 2,
            (counts[WordSwapTechnique.CROSS_CHECK] ?: 0) to 2,
            (counts[WordSwapTechnique.ONLY_SLOT] ?: 0) to 2,
            (counts[WordSwapTechnique.CROSSING_SINGLE] ?: 0) to 2,
            entry.rows * entry.cols to 2,
        )
    }
    return tieredRawScore(entry.difficulty, fine)
}
