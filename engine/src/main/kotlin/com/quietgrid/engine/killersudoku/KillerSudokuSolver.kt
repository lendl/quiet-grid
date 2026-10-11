package com.quietgrid.engine.killersudoku

import com.quietgrid.engine.sudoku.FULL_MASK
import com.quietgrid.engine.sudoku.allHouseRefs
import com.quietgrid.engine.sudoku.bitToDigit
import com.quietgrid.engine.sudoku.cellBoxIndexes
import com.quietgrid.engine.sudoku.cellColIndexes
import com.quietgrid.engine.sudoku.cellRowIndexes
import com.quietgrid.engine.sudoku.digitToBit
import com.quietgrid.engine.sudoku.popcount

data class KillerSolutionSearch(val solutions: List<IntArray>, val exhausted: Boolean) {
    val count: Int get() = solutions.size
}

private class KillerSearchState(val layout: KillerCageLayout, board: IntArray) {
    val board = board.copyOf()
    val rowMask = IntArray(9)
    val colMask = IntArray(9)
    val boxMask = IntArray(9)
    val cageUsed = IntArray(layout.cages.size)
    val cageRemainingSum = IntArray(layout.cages.size) { layout.cages[it].sum }
    val cageRemainingCells = IntArray(layout.cages.size) { layout.cages[it].cells.size }
    var consistent = true

    init {
        for (cell in 0 until 81) {
            val digit = this.board[cell]
            if (digit == 0) continue
            if ((candidates(cell) and digitToBit[digit]) == 0) consistent = false
            assign(cell, digit)
        }
    }

    fun candidates(cell: Int): Int {
        val cage = layout.cageOfCell[cell]
        val blocked = rowMask[cellRowIndexes[cell]] or colMask[cellColIndexes[cell]] or boxMask[cellBoxIndexes[cell]] or cageUsed[cage]
        return FULL_MASK and blocked.inv() and cageComboUnion(cageUsed[cage], cageRemainingCells[cage], cageRemainingSum[cage])
    }

    fun assign(cell: Int, digit: Int) {
        val bit = digitToBit[digit]
        val cage = layout.cageOfCell[cell]
        board[cell] = digit
        rowMask[cellRowIndexes[cell]] = rowMask[cellRowIndexes[cell]] or bit
        colMask[cellColIndexes[cell]] = colMask[cellColIndexes[cell]] or bit
        boxMask[cellBoxIndexes[cell]] = boxMask[cellBoxIndexes[cell]] or bit
        cageUsed[cage] = cageUsed[cage] or bit
        cageRemainingSum[cage] -= digit
        cageRemainingCells[cage] -= 1
    }

    fun unassign(cell: Int, digit: Int) {
        val bit = digitToBit[digit].inv()
        val cage = layout.cageOfCell[cell]
        board[cell] = 0
        rowMask[cellRowIndexes[cell]] = rowMask[cellRowIndexes[cell]] and bit
        colMask[cellColIndexes[cell]] = colMask[cellColIndexes[cell]] and bit
        boxMask[cellBoxIndexes[cell]] = boxMask[cellBoxIndexes[cell]] and bit
        cageUsed[cage] = cageUsed[cage] and bit
        cageRemainingSum[cage] += digit
        cageRemainingCells[cage] += 1
    }
}

fun searchKillerSolutions(
    layout: KillerCageLayout,
    limit: Int = 2,
    nodeLimit: Int = 40_000,
    board: IntArray = IntArray(81),
): KillerSolutionSearch {
    val state = KillerSearchState(layout, board)
    if (!state.consistent) return KillerSolutionSearch(emptyList(), exhausted = false)
    val solutions = mutableListOf<IntArray>()
    var nodes = 0
    var exhausted = false

    val candidates = IntArray(81)

    fun solve() {
        if (solutions.size >= limit || exhausted) return
        nodes += 1
        if (nodes > nodeLimit) { exhausted = true; return }

        var anyEmpty = false
        for (cell in 0 until 81) {
            if (state.board[cell] != 0) { candidates[cell] = 0; continue }
            anyEmpty = true
            candidates[cell] = state.candidates(cell)
            if (candidates[cell] == 0) return
        }
        if (anyEmpty && !restrictToCageSupport(state, candidates)) return

        var bestCell = -1
        var bestMask = 0
        var bestCount = 10
        for (cell in 0 until 81) {
            if (state.board[cell] != 0) continue
            val count = popcount[candidates[cell]]
            if (count < bestCount) {
                bestCell = cell
                bestMask = candidates[cell]
                bestCount = count
            }
        }
        if (bestCell == -1) { solutions.add(state.board.copyOf()); return }

        if (bestCount > 1) {
            for (house in allHouseRefs) {
                var once = 0
                var twice = 0
                var placed = 0
                for (cell in house.cells) {
                    val digit = state.board[cell]
                    if (digit != 0) { placed = placed or digitToBit[digit]; continue }
                    twice = twice or (once and candidates[cell])
                    once = once or candidates[cell]
                }
                if ((once or placed) != FULL_MASK) return
                val single = once and twice.inv()
                if (single != 0 && bestCount > 1) {
                    val bit = single and -single
                    bestCell = house.cells.first { state.board[it] == 0 && (candidates[it] and bit) != 0 }
                    bestMask = bit
                    bestCount = 1
                }
            }
        }

        var remaining = bestMask
        while (remaining != 0) {
            val bit = remaining and -remaining
            remaining = remaining xor bit
            val digit = bitToDigit[bit]
            state.assign(bestCell, digit)
            solve()
            state.unassign(bestCell, digit)
            if (solutions.size >= limit || exhausted) return
        }
    }

    solve()
    return KillerSolutionSearch(solutions, exhausted)
}

private fun restrictToCageSupport(state: KillerSearchState, candidates: IntArray): Boolean {
    for (cage in state.layout.cages.indices) {
        val empty = state.layout.cages[cage].cells.filter { state.board[it] == 0 }
        if (empty.isEmpty()) continue
        val result = computeCageSupport(IntArray(empty.size) { candidates[empty[it]] }, state.cageUsed[cage], state.layout.cages[cage].sum) ?: return false
        for (i in empty.indices) {
            candidates[empty[i]] = candidates[empty[i]] and result.support[i]
            if (candidates[empty[i]] == 0) return false
        }
    }
    return true
}
