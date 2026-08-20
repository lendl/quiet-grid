package com.quietgrid.app.games.sudoku

import com.quietgrid.engine.sudoku.SudokuCandidateEliminationMove
import com.quietgrid.engine.sudoku.SudokuGrid
import com.quietgrid.engine.sudoku.SudokuHouseRef
import com.quietgrid.engine.sudoku.SudokuPlacementMove
import com.quietgrid.engine.sudoku.SudokuTechnique
import com.quietgrid.engine.sudoku.findNextMove

data class SudokuHouseRefUi(val kind: String, val index: Int)

sealed interface SudokuNextMoveHint {
    val evidenceCells: List<Pair<Int, Int>>
    val targetCells: List<Pair<Int, Int>>
    val highlightRows: List<Int>
    val highlightCols: List<Int>
    val highlightBoxes: List<Int>
}

data class SudokuInvalidConflict(
    val house: SudokuHouseRefUi,
    val digit: Int,
    override val evidenceCells: List<Pair<Int, Int>>,
    override val highlightRows: List<Int>,
    override val highlightCols: List<Int>,
    override val highlightBoxes: List<Int>,
) : SudokuNextMoveHint {
    override val targetCells = emptyList<Pair<Int, Int>>()
}

data class SudokuInvalidDeadCell(
    val row: Int,
    val col: Int,
    override val evidenceCells: List<Pair<Int, Int>>,
    override val highlightRows: List<Int>,
    override val highlightCols: List<Int>,
    override val highlightBoxes: List<Int>,
) : SudokuNextMoveHint {
    override val targetCells = emptyList<Pair<Int, Int>>()
}

data class SudokuPlacementHint(
    val technique: SudokuTechnique,
    val row: Int,
    val col: Int,
    val digit: Int,
    val house: SudokuHouseRefUi?,
    override val evidenceCells: List<Pair<Int, Int>>,
    override val targetCells: List<Pair<Int, Int>>,
    override val highlightRows: List<Int>,
    override val highlightCols: List<Int>,
    override val highlightBoxes: List<Int>,
) : SudokuNextMoveHint

data class SudokuEliminationHint(
    val technique: SudokuTechnique,
    val digits: List<Int>,
    val sourceHouse: SudokuHouseRefUi?,
    val targetHouse: SudokuHouseRefUi?,
    override val evidenceCells: List<Pair<Int, Int>>,
    override val targetCells: List<Pair<Int, Int>>,
    override val highlightRows: List<Int>,
    override val highlightCols: List<Int>,
    override val highlightBoxes: List<Int>,
) : SudokuNextMoveHint

private fun cellBox(row: Int, col: Int): Int = (row / 3) * 3 + (col / 3)

private fun findDuplicateConflict(board: SudokuGrid): SudokuInvalidConflict? {
    for (row in 0 until 9) {
        val byDigit = HashMap<Int, MutableList<Pair<Int, Int>>>()
        for (col in 0 until 9) board[row][col]?.let { byDigit.getOrPut(it) { mutableListOf() }.add(row to col) }
        val conflict = byDigit.entries.firstOrNull { it.value.size >= 2 } ?: continue
        return SudokuInvalidConflict(SudokuHouseRefUi("row", row), conflict.key, conflict.value, listOf(row), emptyList(), emptyList())
    }
    for (col in 0 until 9) {
        val byDigit = HashMap<Int, MutableList<Pair<Int, Int>>>()
        for (row in 0 until 9) board[row][col]?.let { byDigit.getOrPut(it) { mutableListOf() }.add(row to col) }
        val conflict = byDigit.entries.firstOrNull { it.value.size >= 2 } ?: continue
        return SudokuInvalidConflict(SudokuHouseRefUi("column", col), conflict.key, conflict.value, emptyList(), listOf(col), emptyList())
    }
    for (box in 0 until 9) {
        val rowStart = (box / 3) * 3
        val colStart = (box % 3) * 3
        val byDigit = HashMap<Int, MutableList<Pair<Int, Int>>>()
        for (r in rowStart until rowStart + 3) for (c in colStart until colStart + 3) {
            board[r][c]?.let { byDigit.getOrPut(it) { mutableListOf() }.add(r to c) }
        }
        val conflict = byDigit.entries.firstOrNull { it.value.size >= 2 } ?: continue
        return SudokuInvalidConflict(SudokuHouseRefUi("box", box), conflict.key, conflict.value, emptyList(), emptyList(), listOf(box))
    }
    return null
}

private fun findDeadCellConflict(board: SudokuGrid): SudokuInvalidDeadCell? {
    for (row in 0 until 9) {
        for (col in 0 until 9) {
            if (board[row][col] != null) continue
            val used = (0 until 9).mapNotNull { board[row][it] }.toSet() +
                (0 until 9).mapNotNull { board[it][col] }.toSet() +
                run {
                    val rowStart = (row / 3) * 3; val colStart = (col / 3) * 3
                    (rowStart until rowStart + 3).flatMap { r -> (colStart until colStart + 3).mapNotNull { c -> board[r][c] } }
                }.toSet()
            if (used.size < 9) continue
            val evidence = ((0 until 9).mapNotNull { c -> board[row][c]?.let { row to c } } +
                (0 until 9).mapNotNull { r -> board[r][col]?.let { r to col } }).distinct()
            return SudokuInvalidDeadCell(row, col, evidence, listOf(row), listOf(col), listOf(cellBox(row, col)))
        }
    }
    return null
}

fun findSudokuInvalidState(board: SudokuGrid): SudokuNextMoveHint? = findDuplicateConflict(board) ?: findDeadCellConflict(board)

private fun highlightsFor(houses: List<SudokuHouseRef>): Triple<List<Int>, List<Int>, List<Int>> = Triple(
    houses.filter { it.kind == "row" }.map { it.index }.distinct().sorted(),
    houses.filter { it.kind == "column" }.map { it.index }.distinct().sorted(),
    houses.filter { it.kind == "box" }.map { it.index }.distinct().sorted(),
)

private fun SudokuHouseRef.toUi(): SudokuHouseRefUi = SudokuHouseRefUi(kind, index)

fun getSudokuNextMoveHint(board: SudokuGrid): SudokuNextMoveHint? {
    findSudokuInvalidState(board)?.let { return it }

    val move = findNextMove(board) ?: return null
    return when (move) {
        is SudokuPlacementMove -> {
            val (rows, cols, boxes) = highlightsFor(move.houses)
            SudokuPlacementHint(
                technique = move.technique,
                row = move.targetRow,
                col = move.targetCol,
                digit = move.digit,
                house = move.houses.firstOrNull()?.toUi(),
                evidenceCells = move.evidenceCells.map { it.row to it.col },
                targetCells = listOf(move.targetRow to move.targetCol),
                highlightRows = rows,
                highlightCols = cols,
                highlightBoxes = boxes,
            )
        }
        is SudokuCandidateEliminationMove -> {
            val (rows, cols, boxes) = highlightsFor(move.houses)
            SudokuEliminationHint(
                technique = move.technique,
                digits = move.eliminations.map { it.third }.distinct(),
                sourceHouse = move.houses.getOrNull(0)?.toUi(),
                targetHouse = move.houses.getOrNull(1)?.toUi(),
                evidenceCells = move.evidenceCells.map { it.row to it.col },
                targetCells = move.eliminations.map { it.first to it.second }.distinct(),
                highlightRows = rows,
                highlightCols = cols,
                highlightBoxes = boxes,
            )
        }
    }
}
