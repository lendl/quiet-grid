package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

object PointingPairTripleTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.POINTING_PAIR_TRIPLE
    override val tier = Difficulty.MEDIUM

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        var best: SudokuCanonicalMove? = null
        var bestComplexity = Int.MAX_VALUE

        for (boxIndex in boxCellIndexes.indices) {
            val boxCells = boxCellIndexes[boxIndex]
            val complexity = boxCells.count { state.board[it] == 0 }
            if (complexity >= bestComplexity) continue

            for (digit in 1..9) {
                val matches = getHouseDigitMatches(state, boxCells, digit)
                if (matches.size !in 2..3) continue

                val distinctRows = matches.map { cellRowIndexes[it] }.distinct()
                if (distinctRows.size == 1) {
                    val targetRow = distinctRows[0]
                    val eliminations = rowCellIndexes[targetRow]
                        .filter { cellBoxIndexes[it] != boxIndex && state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                        .map { it to digit }
                    val move = buildCandidateEliminationMove(
                        SudokuTechnique.POINTING_PAIR_TRIPLE, eliminations, matches,
                        listOf(SudokuHouseRef("box", boxIndex), SudokuHouseRef("row", targetRow)), complexity,
                    )
                    if (move != null) { best = move; bestComplexity = complexity; break }
                }

                val distinctCols = matches.map { cellColIndexes[it] }.distinct()
                if (distinctCols.size == 1) {
                    val targetCol = distinctCols[0]
                    val eliminations = columnCellIndexes[targetCol]
                        .filter { cellBoxIndexes[it] != boxIndex && state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                        .map { it to digit }
                    val move = buildCandidateEliminationMove(
                        SudokuTechnique.POINTING_PAIR_TRIPLE, eliminations, matches,
                        listOf(SudokuHouseRef("box", boxIndex), SudokuHouseRef("column", targetCol)), complexity,
                    )
                    if (move != null) { best = move; bestComplexity = complexity; break }
                }
            }
        }
        return best
    }
}
