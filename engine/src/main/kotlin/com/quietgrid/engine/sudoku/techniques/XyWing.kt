package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

object XyWingTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.XY_WING
    override val tier = Difficulty.HARD

    private data class BivalueCell(val row: Int, val col: Int, val index: Int, val candidates: List<Int>)

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        val bivalueCells = state.board.indices
            .filter { state.board[it] == 0 && popcount[state.candidateMask[it]] == 2 }
            .map { BivalueCell(it / 9, it % 9, it, getCellCandidatesByIndex(state, it)) }
        val complexity = bivalueCells.size

        for (pivot in bivalueCells) {
            val wings = bivalueCells.filter { it.index != pivot.index && arePeerIndexes(it.index, pivot.index) }

            for (leftWing in wings) {
                for (rightWing in wings) {
                    if (leftWing.index == rightWing.index) continue
                    val leftShared = pivot.candidates.filter { leftWing.candidates.contains(it) }
                    val rightShared = pivot.candidates.filter { rightWing.candidates.contains(it) }
                    if (leftShared.size != 1 || rightShared.size != 1 || leftShared[0] == rightShared[0]) continue

                    val leftExtra = leftWing.candidates.filter { !pivot.candidates.contains(it) }
                    val rightExtra = rightWing.candidates.filter { !pivot.candidates.contains(it) }
                    if (leftExtra.size != 1 || rightExtra.size != 1) continue
                    val zDigit = leftExtra[0]
                    if (zDigit != rightExtra[0]) continue

                    val eliminations = state.board.indices
                        .filter { state.board[it] == 0 && it != pivot.index && it != leftWing.index && it != rightWing.index }
                        .filter { index ->
                            val cell = SudokuCellRef(index / 9, index % 9)
                            arePeerCells(cell, SudokuCellRef(leftWing.row, leftWing.col)) && arePeerCells(cell, SudokuCellRef(rightWing.row, rightWing.col))
                        }
                        .filter { getCellCandidatesByIndex(state, it).contains(zDigit) }
                        .map { it to zDigit }

                    val move = buildCandidateEliminationMove(
                        SudokuTechnique.XY_WING, eliminations,
                        listOf(pivot.index, leftWing.index, rightWing.index),
                        buildPlacementHouseRefs(pivot.row, pivot.col), complexity,
                    )
                    if (move != null) return move
                }
            }
        }
        return null
    }
}
