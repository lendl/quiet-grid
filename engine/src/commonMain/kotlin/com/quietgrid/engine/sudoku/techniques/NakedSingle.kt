package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

private fun computeComplexity(state: SudokuBitmaskState, index: Int): Int {
    val layout = state.layout
    val board = state.board
    val rowEmpty = layout.rowCellIndexes[layout.cellRowIndexes[index]].count { board[it] == 0 && it != index }
    val colEmpty = layout.columnCellIndexes[layout.cellColIndexes[index]].count { board[it] == 0 && it != index }
    val boxEmpty = layout.boxCellIndexes[layout.cellBoxIndexes[index]].count { board[it] == 0 && it != index }
    return minOf(rowEmpty, colEmpty, boxEmpty)
}

object NakedSingleTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.NAKED_SINGLE
    override val tier = Difficulty.EASY

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        val layout = state.layout
        var best: SudokuPlacementMove? = null
        var bestComplexity = Int.MAX_VALUE

        for (index in state.board.indices) {
            if (state.board[index] != 0 || popcount[state.candidateMask[index]] != 1) continue
            val complexity = computeComplexity(state, index)
            if (complexity >= bestComplexity) continue
            bestComplexity = complexity
            best = buildPlacementMove(
                technique = SudokuTechnique.NAKED_SINGLE,
                row = layout.rowOf(index),
                col = layout.colOf(index),
                digit = bitToDigit[state.candidateMask[index]],
                evidenceCells = listOf(index),
                complexity = complexity,
                layout = layout,
            )
        }
        return best
    }
}
