package com.quietgrid.engine.killersudoku

import com.quietgrid.engine.sudoku.SudokuBitmaskState
import com.quietgrid.engine.sudoku.SudokuHouseRef
import com.quietgrid.engine.sudoku.createBitmaskStateFromFlatBoard
import com.quietgrid.engine.sudoku.digitToBit
import com.quietgrid.engine.sudoku.eliminateCandidate
import com.quietgrid.engine.sudoku.placeDigit

data class KillerMove(
    val technique: KillerTechnique,
    val complexity: Int,
    val placementCell: Int?,
    val placementDigit: Int?,
    val eliminations: List<Pair<Int, Int>>,
    val evidenceCells: List<Int>,
    val cages: List<Int>,
    val houses: List<SudokuHouseRef>,
) {
    val isPlacement: Boolean get() = placementCell != null
    val score: Int get() = technique.baseScore + complexity
}

class KillerSolverState(val grid: SudokuBitmaskState, val layout: KillerCageLayout) {
    var regionMoves: List<KillerMove>? = null

    fun cagePlacedMask(cage: Int): Int = layout.cages[cage].cells.fold(0) { mask, cell -> mask or digitToBit[grid.board[cell]] }

    fun cageRemainingSum(cage: Int): Int = layout.cages[cage].sum - layout.cages[cage].cells.sumOf { grid.board[it] }

    fun cageEmptyCells(cage: Int): List<Int> = layout.cages[cage].cells.filter { grid.board[it] == 0 }
}

fun createKillerSolverState(layout: KillerCageLayout, board: IntArray = IntArray(81)): KillerSolverState {
    val state = KillerSolverState(createBitmaskStateFromFlatBoard(board), layout)
    for (cell in 0 until 81) {
        val digit = board[cell]
        if (digit != 0) layout.cageMates(cell).forEach { eliminateCandidate(state.grid, it, digit) }
    }
    return state
}

fun killerPlaceDigit(state: KillerSolverState, cell: Int, digit: Int) {
    state.regionMoves = null
    placeDigit(state.grid, cell, digit)
    state.layout.cageMates(cell).forEach { eliminateCandidate(state.grid, it, digit) }
}

fun applyKillerMove(state: KillerSolverState, move: KillerMove) {
    if (move.placementCell != null && move.placementDigit != null) {
        killerPlaceDigit(state, move.placementCell, move.placementDigit)
    } else {
        state.regionMoves = null
        move.eliminations.forEach { (cell, digit) -> eliminateCandidate(state.grid, cell, digit) }
    }
}

fun killerBasicCandidateMasks(layout: KillerCageLayout, board: IntArray): IntArray {
    val state = createKillerSolverState(layout, board)
    return IntArray(81) { cell ->
        if (board[cell] != 0) {
            0
        } else {
            val cage = layout.cageOfCell[cell]
            state.grid.candidateMask[cell] and cageComboUnion(state.cagePlacedMask(cage), state.cageEmptyCells(cage).size, state.cageRemainingSum(cage))
        }
    }
}

fun findKillerCageConflict(layout: KillerCageLayout, board: IntArray): Int? {
    layout.cages.forEachIndexed { index, cage ->
        val digits = cage.cells.map { board[it] }.filter { it != 0 }
        if (digits.size != digits.distinct().size) return index
        val used = digits.fold(0) { mask, digit -> mask or digitToBit[digit] }
        if (!isCageRemainderFeasible(used, cage.cells.size - digits.size, cage.sum - digits.sum())) return index
    }
    return null
}
