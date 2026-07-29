package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

object BoxLineReductionTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.BOX_LINE_REDUCTION
    override val tier = Difficulty.MEDIUM

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        var best: SudokuCanonicalMove? = null
        var bestComplexity = Int.MAX_VALUE

        for (rowIndex in rowCellIndexes.indices) {
            val rowCells = rowCellIndexes[rowIndex]
            val complexity = rowCells.count { state.board[it] == 0 }
            if (complexity >= bestComplexity) continue

            for (digit in 1..9) {
                val matches = getHouseDigitMatches(state, rowCells, digit)
                if (matches.size !in 2..3) continue
                val boxIndexes = matches.map { cellBoxIndexes[it] }.distinct()
                if (boxIndexes.size != 1) continue
                val boxIndex = boxIndexes[0]
                val eliminations = boxCellIndexes[boxIndex]
                    .filter { it / 9 != rowIndex && state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                    .map { it to digit }
                val move = buildCandidateEliminationMove(
                    SudokuTechnique.BOX_LINE_REDUCTION, eliminations, matches,
                    listOf(SudokuHouseRef("row", rowIndex), SudokuHouseRef("box", boxIndex)), complexity,
                )
                if (move != null) { best = move; bestComplexity = complexity; break }
            }
        }

        for (colIndex in columnCellIndexes.indices) {
            val colCells = columnCellIndexes[colIndex]
            val complexity = colCells.count { state.board[it] == 0 }
            if (complexity >= bestComplexity) continue

            for (digit in 1..9) {
                val matches = getHouseDigitMatches(state, colCells, digit)
                if (matches.size !in 2..3) continue
                val boxIndexes = matches.map { cellBoxIndexes[it] }.distinct()
                if (boxIndexes.size != 1) continue
                val boxIndex = boxIndexes[0]
                val eliminations = boxCellIndexes[boxIndex]
                    .filter { it % 9 != colIndex && state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                    .map { it to digit }
                val move = buildCandidateEliminationMove(
                    SudokuTechnique.BOX_LINE_REDUCTION, eliminations, matches,
                    listOf(SudokuHouseRef("column", colIndex), SudokuHouseRef("box", boxIndex)), complexity,
                )
                if (move != null) { best = move; bestComplexity = complexity; break }
            }
        }
        return best
    }
}
