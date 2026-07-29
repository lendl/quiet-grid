package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

private fun computeComplexity(board: IntArray, index: Int): Int {
    val row = index / 9
    val col = index % 9
    val box = cellBoxIndexes[index]
    val rowEmpty = rowCellIndexes[row].count { board[it] == 0 && it != index }
    val colEmpty = columnCellIndexes[col].count { board[it] == 0 && it != index }
    val boxEmpty = boxCellIndexes[box].count { board[it] == 0 && it != index }
    return minOf(rowEmpty, colEmpty, boxEmpty)
}

object NakedSingleTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.NAKED_SINGLE
    override val tier = Difficulty.EASY

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        var best: SudokuPlacementMove? = null
        var bestComplexity = Int.MAX_VALUE

        for (index in state.board.indices) {
            if (state.board[index] != 0 || popcount[state.candidateMask[index]] != 1) continue
            val complexity = computeComplexity(state.board, index)
            if (complexity >= bestComplexity) continue
            bestComplexity = complexity
            best = buildPlacementMove(
                technique = SudokuTechnique.NAKED_SINGLE,
                row = index / 9,
                col = index % 9,
                digit = bitToDigit[state.candidateMask[index]],
                evidenceCells = listOf(index),
                complexity = complexity,
            )
        }
        return best
    }
}
