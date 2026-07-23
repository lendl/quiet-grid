package com.quietgrid.app.games.sudoku

typealias SudokuUnitKey = String

enum class SudokuUnitState { INCOMPLETE, CORRECT, INCORRECT }

fun sudokuBoxIndex(row: Int, col: Int): Int = (row / 3) * 3 + (col / 3)

fun sudokuTouchedUnitKeys(row: Int, col: Int): List<SudokuUnitKey> =
    listOf("r$row", "c$col", "b${sudokuBoxIndex(row, col)}")

private fun unitCellIndexes(unitKey: SudokuUnitKey): List<Pair<Int, Int>> {
    val index = unitKey.substring(1).toInt()
    return when (unitKey[0]) {
        'r' -> (0 until 9).map { c -> index to c }
        'c' -> (0 until 9).map { r -> r to index }
        else -> {
            val rowStart = (index / 3) * 3
            val colStart = (index % 3) * 3
            (0 until 9).map { offset -> (rowStart + offset / 3) to (colStart + offset % 3) }
        }
    }
}

fun getCompletedSudokuUnitState(board: SudokuGrid, solution: List<List<Int>>, unitKey: SudokuUnitKey): SudokuUnitState {
    val cells = unitCellIndexes(unitKey)
    if (cells.any { (r, c) -> board[r][c] == null }) return SudokuUnitState.INCOMPLETE
    return if (cells.all { (r, c) -> board[r][c] == solution[r][c] }) SudokuUnitState.CORRECT else SudokuUnitState.INCORRECT
}

private fun collectUnitKeys(board: SudokuGrid, solution: List<List<Int>>, target: SudokuUnitState): List<SudokuUnitKey> {
    val keys = mutableListOf<SudokuUnitKey>()
    for (i in 0 until 9) {
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
