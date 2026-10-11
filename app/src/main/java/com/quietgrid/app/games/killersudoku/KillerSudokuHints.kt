package com.quietgrid.app.games.killersudoku

import com.quietgrid.app.games.sudoku.SudokuNextMoveHint
import com.quietgrid.app.games.sudoku.findSudokuInvalidState
import com.quietgrid.engine.killersudoku.KillerCageLayout
import com.quietgrid.engine.killersudoku.KillerTechnique
import com.quietgrid.engine.killersudoku.findKillerCageConflict
import com.quietgrid.engine.killersudoku.findKillerHint
import com.quietgrid.engine.sudoku.SudokuGrid
import com.quietgrid.engine.sudoku.SudokuHouseRef

sealed interface KillerSudokuHint {
    val targetCells: Set<Pair<Int, Int>>
    val evidenceCells: Set<Pair<Int, Int>>
    val highlightCages: Set<Int>
    val highlightRows: Set<Int>
    val highlightCols: Set<Int>
    val highlightBoxes: Set<Int>
}

data class KillerSudokuGridConflictHint(val conflict: SudokuNextMoveHint) : KillerSudokuHint {
    override val targetCells = emptySet<Pair<Int, Int>>()
    override val evidenceCells = conflict.evidenceCells.toSet()
    override val highlightCages = emptySet<Int>()
    override val highlightRows = conflict.highlightRows.toSet()
    override val highlightCols = conflict.highlightCols.toSet()
    override val highlightBoxes = conflict.highlightBoxes.toSet()
}

data class KillerSudokuCageConflictHint(val cage: Int, val sum: Int) : KillerSudokuHint {
    override val targetCells = emptySet<Pair<Int, Int>>()
    override val evidenceCells = emptySet<Pair<Int, Int>>()
    override val highlightCages = setOf(cage)
    override val highlightRows = emptySet<Int>()
    override val highlightCols = emptySet<Int>()
    override val highlightBoxes = emptySet<Int>()
}

data class KillerSudokuPlacementHint(
    val row: Int,
    val col: Int,
    val digit: Int,
    val technique: KillerTechnique,
    val cageSum: Int?,
    val cageSize: Int?,
    val houses: List<SudokuHouseRef>,
    override val highlightCages: Set<Int>,
) : KillerSudokuHint {
    override val targetCells = setOf(row to col)
    override val evidenceCells = emptySet<Pair<Int, Int>>()
    override val highlightRows = houses.filter { it.kind == "row" }.map { it.index }.toSet()
    override val highlightCols = houses.filter { it.kind == "column" }.map { it.index }.toSet()
    override val highlightBoxes = houses.filter { it.kind == "box" }.map { it.index }.toSet()
}

fun getKillerSudokuHint(layout: KillerCageLayout, board: SudokuGrid): KillerSudokuHint? {
    findSudokuInvalidState(board)?.let { return KillerSudokuGridConflictHint(it) }
    val flat = killerBoardArray(board)
    findKillerCageConflict(layout, flat)?.let { return KillerSudokuCageConflictHint(it, layout.cages[it].sum) }
    val hint = runCatching { findKillerHint(layout, flat) }.getOrNull() ?: return null
    val key = hint.keyMove
    val keyCage = key.cages.firstOrNull()?.let { layout.cages[it] }
    return KillerSudokuPlacementHint(
        row = hint.cell / 9,
        col = hint.cell % 9,
        digit = hint.digit,
        technique = key.technique,
        cageSum = keyCage?.sum,
        cageSize = keyCage?.cells?.size,
        houses = key.houses,
        highlightCages = key.cages.toSet(),
    )
}
