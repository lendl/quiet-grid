package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

object HiddenPairTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.HIDDEN_PAIR
    override val tier = Difficulty.MEDIUM

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        var best: SudokuCanonicalMove? = null
        var bestComplexity = Double.MAX_VALUE

        for (ref in allHouseRefs) {
            val complexity = ref.cells.count { state.board[it] == 0 } * 1.5
            if (complexity >= bestComplexity) continue

            val digitPositions = (1..9).associateWith { digit -> getHouseDigitMatches(state, ref.cells, digit) }
            var found = false
            for (leftIndex in 1..9) {
                if (found) break
                for (rightIndex in (leftIndex + 1)..9) {
                    val leftPositions = digitPositions.getValue(leftIndex)
                    val rightPositions = digitPositions.getValue(rightIndex)
                    if (leftPositions.size != 2 || rightPositions.size != 2) continue
                    if (leftPositions[0] != rightPositions[0] || leftPositions[1] != rightPositions[1]) continue

                    val eliminations = leftPositions.flatMap { index ->
                        getCellCandidatesByIndex(state, index).filter { it != leftIndex && it != rightIndex }.map { index to it }
                    }
                    val move = buildCandidateEliminationMove(SudokuTechnique.HIDDEN_PAIR, eliminations, leftPositions, listOf(ref.house), complexity.toInt())
                    if (move != null) {
                        best = move
                        bestComplexity = complexity
                        found = true
                        break
                    }
                }
            }
        }
        return best
    }
}
