package com.quietgrid.engine.arrowescape

import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.journey.mapCell
import com.quietgrid.engine.journey.mapVector
import com.quietgrid.engine.journey.outputSize

fun arrowEscapeReplayCandidates(entry: ArrowEscapePuzzleEntry): List<GridTransform> = listOf(GridTransform.MIRROR_H)

fun ArrowEscapePuzzleEntry.transformed(t: GridTransform): ArrowEscapePuzzleEntry {
    if (t == GridTransform.IDENTITY) return this
    val (newRows, newCols) = t.outputSize(rows, cols)
    val newMask = mask?.let { maskRows ->
        val grid = Array(newRows) { CharArray(newCols) { ARROW_ESCAPE_MASK_VOID } }
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val (r, c) = t.mapCell(row, col, rows, cols)
                grid[r][c] = maskRows.getOrNull(row)?.getOrNull(col) ?: ARROW_ESCAPE_MASK_VOID
            }
        }
        grid.map { String(it) }
    }
    return copy(
        rows = newRows,
        cols = newCols,
        pieces = pieces.map { piece ->
            ArrowEscapePieceData(
                cells = piece.cells.map { cell -> t.mapCell(cell[0], cell[1], rows, cols).toList() },
                headDirection = transformDirection(arrowDirectionFromKey(piece.headDirection), t).key,
            )
        },
        mask = newMask,
    )
}

private fun transformDirection(direction: ArrowDirection, t: GridTransform): ArrowDirection {
    val (dRow, dCol) = when (direction) {
        ArrowDirection.UP -> -1 to 0
        ArrowDirection.DOWN -> 1 to 0
        ArrowDirection.LEFT -> 0 to -1
        ArrowDirection.RIGHT -> 0 to 1
    }
    return when (t.mapVector(dRow, dCol)) {
        -1 to 0 -> ArrowDirection.UP
        1 to 0 -> ArrowDirection.DOWN
        0 to -1 -> ArrowDirection.LEFT
        else -> ArrowDirection.RIGHT
    }
}
