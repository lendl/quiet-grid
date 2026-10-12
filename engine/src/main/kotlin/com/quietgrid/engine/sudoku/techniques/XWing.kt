package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

object XWingTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.X_WING
    override val tier = Difficulty.HARD

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        val layout = state.layout
        val size = layout.size
        var best: SudokuCanonicalMove? = null
        var bestComplexity = Int.MAX_VALUE

        for (digit in layout.digits) {
            val rowCandidates = layout.rowCellIndexes.mapIndexed { row, cells -> row to getHouseDigitMatches(state, cells, digit).map { it % size } }
                .filter { it.second.size == 2 }
            val rowComplexity = rowCandidates.size
            if (rowComplexity < bestComplexity) {
                outer@ for (first in rowCandidates.indices) {
                    for (second in (first + 1) until rowCandidates.size) {
                        val (leftRow, leftCols) = rowCandidates[first]
                        val (rightRow, rightCols) = rowCandidates[second]
                        if (leftCols[0] != rightCols[0] || leftCols[1] != rightCols[1]) continue
                        val eliminations = leftCols.flatMap { col ->
                            layout.columnCellIndexes[col].filter { it / size != leftRow && it / size != rightRow }
                                .filter { state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                                .map { it to digit }
                        }
                        val move = buildCandidateEliminationMove(
                            SudokuTechnique.X_WING, eliminations,
                            listOf(leftRow * size + leftCols[0], leftRow * size + leftCols[1], rightRow * size + rightCols[0], rightRow * size + rightCols[1]),
                            listOf(SudokuHouseRef("row", leftRow), SudokuHouseRef("row", rightRow), SudokuHouseRef("column", leftCols[0]), SudokuHouseRef("column", leftCols[1])),
                            rowComplexity, layout,
                        )
                        if (move != null) { best = move; bestComplexity = rowComplexity; break@outer }
                    }
                }
            }

            val columnCandidates = layout.columnCellIndexes.mapIndexed { col, cells -> col to getHouseDigitMatches(state, cells, digit).map { it / size } }
                .filter { it.second.size == 2 }
            val colComplexity = columnCandidates.size
            if (colComplexity < bestComplexity) {
                outer@ for (first in columnCandidates.indices) {
                    for (second in (first + 1) until columnCandidates.size) {
                        val (leftCol, leftRows) = columnCandidates[first]
                        val (rightCol, rightRows) = columnCandidates[second]
                        if (leftRows[0] != rightRows[0] || leftRows[1] != rightRows[1]) continue
                        val eliminations = leftRows.flatMap { row ->
                            layout.rowCellIndexes[row].filter { it % size != leftCol && it % size != rightCol }
                                .filter { state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                                .map { it to digit }
                        }
                        val move = buildCandidateEliminationMove(
                            SudokuTechnique.X_WING, eliminations,
                            listOf(leftRows[0] * size + leftCol, leftRows[1] * size + leftCol, rightRows[0] * size + rightCol, rightRows[1] * size + rightCol),
                            listOf(SudokuHouseRef("column", leftCol), SudokuHouseRef("column", rightCol), SudokuHouseRef("row", leftRows[0]), SudokuHouseRef("row", leftRows[1])),
                            colComplexity, layout,
                        )
                        if (move != null) { best = move; bestComplexity = colComplexity; break@outer }
                    }
                }
            }
        }
        return best
    }
}
