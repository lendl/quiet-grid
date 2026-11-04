package com.quietgrid.engine.killersudoku

import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.journey.gridTransformsFor
import com.quietgrid.engine.journey.movedBy
import com.quietgrid.engine.journey.mapCell
import com.quietgrid.engine.sudoku.SUDOKU_SIZE

fun killerSudokuReplayCandidates(entry: KillerSudokuPuzzleEntry): List<GridTransform> =
    gridTransformsFor(SUDOKU_SIZE, SUDOKU_SIZE) - GridTransform.IDENTITY

fun KillerSudokuPuzzleEntry.transformed(t: GridTransform): KillerSudokuPuzzleEntry {
    if (t == GridTransform.IDENTITY) return this
    val movedCages = cages.map { cage ->
        cage.copy(
            cells = cage.cells.map { cell ->
                val (row, col) = t.mapCell(cell / SUDOKU_SIZE, cell % SUDOKU_SIZE, SUDOKU_SIZE, SUDOKU_SIZE)
                row * SUDOKU_SIZE + col
            }.sorted(),
        )
    }
    return copy(cages = movedCages, solution = solution.movedBy(t))
}
