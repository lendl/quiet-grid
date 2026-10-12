package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

object BoxLineReductionTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.BOX_LINE_REDUCTION
    override val tier = Difficulty.MEDIUM

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        val layout = state.layout
        var best: SudokuCanonicalMove? = null
        var bestComplexity = Int.MAX_VALUE

        for (rowIndex in layout.rowCellIndexes.indices) {
            val rowCells = layout.rowCellIndexes[rowIndex]
            val complexity = rowCells.count { state.board[it] == 0 }
            if (complexity >= bestComplexity) continue

            for (digit in layout.digits) {
                val matches = getHouseDigitMatches(state, rowCells, digit)
                if (matches.size !in 2..3) continue
                val boxIndexes = matches.map { layout.cellBoxIndexes[it] }.distinct()
                if (boxIndexes.size != 1) continue
                val boxIndex = boxIndexes[0]
                val eliminations = layout.boxCellIndexes[boxIndex]
                    .filter { layout.rowOf(it) != rowIndex && state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                    .map { it to digit }
                val move = buildCandidateEliminationMove(
                    SudokuTechnique.BOX_LINE_REDUCTION, eliminations, matches,
                    listOf(SudokuHouseRef("row", rowIndex), SudokuHouseRef("box", boxIndex)), complexity, layout,
                )
                if (move != null) { best = move; bestComplexity = complexity; break }
            }
        }

        for (colIndex in layout.columnCellIndexes.indices) {
            val colCells = layout.columnCellIndexes[colIndex]
            val complexity = colCells.count { state.board[it] == 0 }
            if (complexity >= bestComplexity) continue

            for (digit in layout.digits) {
                val matches = getHouseDigitMatches(state, colCells, digit)
                if (matches.size !in 2..3) continue
                val boxIndexes = matches.map { layout.cellBoxIndexes[it] }.distinct()
                if (boxIndexes.size != 1) continue
                val boxIndex = boxIndexes[0]
                val eliminations = layout.boxCellIndexes[boxIndex]
                    .filter { layout.colOf(it) != colIndex && state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                    .map { it to digit }
                val move = buildCandidateEliminationMove(
                    SudokuTechnique.BOX_LINE_REDUCTION, eliminations, matches,
                    listOf(SudokuHouseRef("column", colIndex), SudokuHouseRef("box", boxIndex)), complexity, layout,
                )
                if (move != null) { best = move; bestComplexity = complexity; break }
            }
        }
        return best
    }
}
