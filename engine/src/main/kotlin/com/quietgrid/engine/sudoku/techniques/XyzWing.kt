package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

object XyzWingTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.XYZ_WING
    override val tier = Difficulty.EXPERT

    private data class Cell(val index: Int, val candidates: List<Int>)

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        val pivotCells = state.board.indices.filter { state.board[it] == 0 && popcount[state.candidateMask[it]] == 3 }
            .map { Cell(it, getCellCandidatesByIndex(state, it)) }
        val wings = state.board.indices.filter { state.board[it] == 0 && popcount[state.candidateMask[it]] == 2 }
            .map { Cell(it, getCellCandidatesByIndex(state, it)) }
        val complexity = pivotCells.size

        for (pivot in pivotCells) {
            val pivotPeers = wings.filter { arePeerIndexes(it.index, pivot.index) }
            for (leftIndex in pivotPeers.indices) {
                for (rightIndex in (leftIndex + 1) until pivotPeers.size) {
                    val leftWing = pivotPeers[leftIndex]
                    val rightWing = pivotPeers[rightIndex]
                    if (!leftWing.candidates.all { pivot.candidates.contains(it) } || !rightWing.candidates.all { pivot.candidates.contains(it) }) continue

                    val wingUnion = (leftWing.candidates + rightWing.candidates).toSortedSet().toList()
                    if (wingUnion.size != 3 || wingUnion.any { !pivot.candidates.contains(it) }) continue

                    val commonDigits = leftWing.candidates.filter { rightWing.candidates.contains(it) }
                    if (commonDigits.size != 1) continue
                    val zDigit = commonDigits[0]

                    val eliminations = peerIntersectionIndexes(listOf(pivot.index, leftWing.index, rightWing.index))
                        .filter { it != pivot.index && it != leftWing.index && it != rightWing.index }
                        .filter { getCellCandidatesByIndex(state, it).contains(zDigit) }
                        .map { it to zDigit }

                    val move = buildCandidateEliminationMove(
                        SudokuTechnique.XYZ_WING, eliminations,
                        listOf(pivot.index, leftWing.index, rightWing.index), null, complexity,
                    )
                    if (move != null) return move
                }
            }
        }
        return null
    }
}
