package com.quietgrid.engine.sudoku

import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.journey.gridTransformsFor
import com.quietgrid.engine.journey.movedBy
import com.quietgrid.engine.journey.outputSize

fun sudokuReplayCandidates(entry: SudokuPuzzleEntry): List<GridTransform> {
    val layout = SudokuLayout.forSize(entry.givens.size)
    return (gridTransformsFor(layout.size, layout.size) - GridTransform.IDENTITY)
        .filter { it.outputSize(layout.boxRows, layout.boxCols) == layout.boxRows to layout.boxCols }
}

fun SudokuPuzzleEntry.transformed(t: GridTransform): SudokuPuzzleEntry {
    if (t == GridTransform.IDENTITY) return this
    return copy(givens = givens.movedBy(t), solution = solution.movedBy(t))
}
