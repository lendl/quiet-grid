package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

object ChainsTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.CHAINS
    override val tier = Difficulty.EXPERT

    private const val MAX_CHAIN_LENGTH = 6

    private data class Cell(val index: Int, val candidates: List<Int>)

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        val bivalueCells = state.board.indices.filter { state.board[it] == 0 && popcount[state.candidateMask[it]] == 2 }
            .map { Cell(it, getCellCandidatesByIndex(state, it)) }

        fun search(startIndex: Int, currentIndex: Int, targetDigit: Int, sharedDigit: Int, path: List<Int>): SudokuCanonicalMove? {
            if (path.size >= MAX_CHAIN_LENGTH) return null

            val nextMoves = bivalueCells.filter { it.index !in path && arePeerIndexes(it.index, currentIndex) && it.candidates.contains(sharedDigit) }

            for (nextCell in nextMoves) {
                val nextDigit = nextCell.candidates.firstOrNull { it != sharedDigit } ?: continue
                val chainLength = path.size + 1

                if (nextCell.candidates.contains(targetDigit) && path.size >= 3) {
                    val fullPath = path + nextCell.index
                    val eliminations = state.board.indices
                        .filter { state.board[it] == 0 && it !in fullPath }
                        .filter { arePeerIndexes(it, startIndex) && arePeerIndexes(it, nextCell.index) }
                        .filter { getCellCandidatesByIndex(state, it).contains(targetDigit) }
                        .map { it to targetDigit }
                    val move = buildCandidateEliminationMove(SudokuTechnique.CHAINS, eliminations, fullPath, null, chainLength)
                    if (move != null) return move
                }

                val nested = search(startIndex, nextCell.index, targetDigit, nextDigit, path + nextCell.index)
                if (nested != null) return nested
            }
            return null
        }

        for (startCell in bivalueCells) {
            val (firstDigit, secondDigit) = startCell.candidates
            val firstMove = search(startCell.index, startCell.index, firstDigit, secondDigit, listOf(startCell.index))
            if (firstMove != null) return firstMove

            val secondMove = search(startCell.index, startCell.index, secondDigit, firstDigit, listOf(startCell.index))
            if (secondMove != null) return secondMove
        }
        return null
    }
}
