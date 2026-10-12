package com.quietgrid.app.games.sudoku

import com.quietgrid.engine.sudoku.SudokuGrid
import com.quietgrid.engine.sudoku.SudokuLayout

typealias SudokuUnitKey = String

enum class SudokuUnitState { INCOMPLETE, CORRECT, INCORRECT }

fun sudokuBoxIndex(row: Int, col: Int, layout: SudokuLayout = SudokuLayout.CLASSIC): Int = layout.boxOf(row, col)

fun sudokuTouchedUnitKeys(row: Int, col: Int, layout: SudokuLayout = SudokuLayout.CLASSIC): List<SudokuUnitKey> =
    listOf("r$row", "c$col", "b${sudokuBoxIndex(row, col, layout)}")

internal fun sudokuUnitCells(unitKey: SudokuUnitKey, layout: SudokuLayout): List<Pair<Int, Int>> {
    val index = unitKey.substring(1).toInt()
    val cells = when (unitKey[0]) {
        'r' -> layout.rowCellIndexes[index]
        'c' -> layout.columnCellIndexes[index]
        else -> layout.boxCellIndexes[index]
    }
    return cells.map { layout.rowOf(it) to layout.colOf(it) }
}

fun getCompletedSudokuUnitState(board: SudokuGrid, solution: List<List<Int>>, unitKey: SudokuUnitKey): SudokuUnitState {
    val cells = sudokuUnitCells(unitKey, SudokuLayout.forSize(board.size))
    if (cells.any { (r, c) -> board[r][c] == null }) return SudokuUnitState.INCOMPLETE
    return if (cells.all { (r, c) -> board[r][c] == solution[r][c] }) SudokuUnitState.CORRECT else SudokuUnitState.INCORRECT
}

private fun collectUnitKeys(board: SudokuGrid, solution: List<List<Int>>, target: SudokuUnitState): List<SudokuUnitKey> {
    val keys = mutableListOf<SudokuUnitKey>()
    for (i in board.indices) {
        for (prefix in listOf("r", "c", "b")) {
            val key = "$prefix$i"
            if (getCompletedSudokuUnitState(board, solution, key) == target) keys.add(key)
        }
    }
    return keys
}

fun getCorrectSudokuUnitKeys(board: SudokuGrid, solution: List<List<Int>>): List<SudokuUnitKey> =
    collectUnitKeys(board, solution, SudokuUnitState.CORRECT)

fun getMismatchedSudokuUnitKeys(board: SudokuGrid, solution: List<List<Int>>): List<SudokuUnitKey> =
    collectUnitKeys(board, solution, SudokuUnitState.INCORRECT)

fun isSudokuSolved(board: SudokuGrid, solution: List<List<Int>>): Boolean =
    board.indices.all { r -> board[r].indices.all { c -> board[r][c] == solution[r][c] } }
