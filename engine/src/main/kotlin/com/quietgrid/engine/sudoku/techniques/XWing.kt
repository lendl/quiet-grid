package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

object XWingTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.X_WING
    override val tier = Difficulty.HARD

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        var best: SudokuCanonicalMove? = null
        var bestComplexity = Int.MAX_VALUE

        for (digit in 1..9) {
            val rowCandidates = rowCellIndexes.mapIndexed { row, cells -> row to getHouseDigitMatches(state, cells, digit).map { it % 9 } }
                .filter { it.second.size == 2 }
            val rowComplexity = rowCandidates.size
            if (rowComplexity < bestComplexity) {
                outer@ for (first in rowCandidates.indices) {
                    for (second in (first + 1) until rowCandidates.size) {
                        val (leftRow, leftCols) = rowCandidates[first]
                        val (rightRow, rightCols) = rowCandidates[second]
                        if (leftCols[0] != rightCols[0] || leftCols[1] != rightCols[1]) continue
                        val eliminations = leftCols.flatMap { col ->
                            columnCellIndexes[col].filter { it / 9 != leftRow && it / 9 != rightRow }
                                .filter { state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                                .map { it to digit }
                        }
                        val move = buildCandidateEliminationMove(
                            SudokuTechnique.X_WING, eliminations,
                            listOf(leftRow * 9 + leftCols[0], leftRow * 9 + leftCols[1], rightRow * 9 + rightCols[0], rightRow * 9 + rightCols[1]),
                            listOf(SudokuHouseRef("row", leftRow), SudokuHouseRef("row", rightRow), SudokuHouseRef("column", leftCols[0]), SudokuHouseRef("column", leftCols[1])),
                            rowComplexity,
                        )
                        if (move != null) { best = move; bestComplexity = rowComplexity; break@outer }
                    }
                }
            }

            val columnCandidates = columnCellIndexes.mapIndexed { col, cells -> col to getHouseDigitMatches(state, cells, digit).map { it / 9 } }
                .filter { it.second.size == 2 }
            val colComplexity = columnCandidates.size
            if (colComplexity < bestComplexity) {
                outer@ for (first in columnCandidates.indices) {
                    for (second in (first + 1) until columnCandidates.size) {
                        val (leftCol, leftRows) = columnCandidates[first]
                        val (rightCol, rightRows) = columnCandidates[second]
                        if (leftRows[0] != rightRows[0] || leftRows[1] != rightRows[1]) continue
                        val eliminations = leftRows.flatMap { row ->
                            rowCellIndexes[row].filter { it % 9 != leftCol && it % 9 != rightCol }
                                .filter { state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                                .map { it to digit }
                        }
                        val move = buildCandidateEliminationMove(
                            SudokuTechnique.X_WING, eliminations,
                            listOf(leftRows[0] * 9 + leftCol, leftRows[1] * 9 + leftCol, rightRows[0] * 9 + rightCol, rightRows[1] * 9 + rightCol),
                            listOf(SudokuHouseRef("column", leftCol), SudokuHouseRef("column", rightCol), SudokuHouseRef("row", leftRows[0]), SudokuHouseRef("row", leftRows[1])),
                            colComplexity,
                        )
                        if (move != null) { best = move; bestComplexity = colComplexity; break@outer }
                    }
                }
            }
        }
        return best
    }
}
