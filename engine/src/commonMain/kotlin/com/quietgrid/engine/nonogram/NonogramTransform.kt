package com.quietgrid.engine.nonogram

import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.journey.gridTransformsFor
import com.quietgrid.engine.journey.movedBy
import com.quietgrid.engine.journey.outputSize

fun nonogramReplayCandidates(entry: NonogramPuzzleEntry): List<GridTransform> =
    gridTransformsFor(entry.rows, entry.cols) - GridTransform.IDENTITY

fun NonogramPuzzleEntry.transformed(t: GridTransform): NonogramPuzzleEntry {
    if (t == GridTransform.IDENTITY) return this
    val (newRows, newCols) = t.outputSize(rows, cols)
    return copy(rows = newRows, cols = newCols, solution = solution.movedBy(t))
}
