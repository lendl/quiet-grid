package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

object NakedPairTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.NAKED_PAIR
    override val tier = Difficulty.EASY

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        var best: SudokuCanonicalMove? = null
        var bestComplexity = Int.MAX_VALUE

        for (ref in allHouseRefs) {
            val complexity = ref.cells.count { state.board[it] == 0 }
            if (complexity >= bestComplexity) continue

            val pairMap = mutableMapOf<Int, MutableList<Int>>()
            ref.cells.forEach { index ->
                val mask = state.candidateMask[index]
                if (state.board[index] != 0 || popcount[mask] != 2) return@forEach
                pairMap.getOrPut(mask) { mutableListOf() }.add(index)
            }

            for ((_, pairCells) in pairMap) {
                if (pairCells.size != 2) continue
                val pairDigits = getCellCandidatesByIndex(state, pairCells[0])
                val eliminations = ref.cells.filter { it !in pairCells }
                    .flatMap { index -> pairDigits.filter { d -> getCellCandidatesByIndex(state, index).contains(d) }.map { index to it } }
                val move = buildCandidateEliminationMove(SudokuTechnique.NAKED_PAIR, eliminations, pairCells, listOf(ref.house), complexity)
                if (move != null) {
                    best = move
                    bestComplexity = complexity
                    break
                }
            }
        }
        return best
    }
}
