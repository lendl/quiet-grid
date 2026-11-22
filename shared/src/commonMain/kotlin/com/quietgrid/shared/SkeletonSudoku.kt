package com.quietgrid.shared

import com.quietgrid.engine.sudoku.SUDOKU_SIZE
import com.quietgrid.engine.sudoku.SudokuGrid
import com.quietgrid.engine.sudoku.SudokuPlacementMove
import com.quietgrid.engine.sudoku.traceHumanSolve

const val SKELETON_PUZZLE = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"

fun solvedSkeletonSudoku(): List<List<Int>> {
    val givens: SudokuGrid = SKELETON_PUZZLE.chunked(SUDOKU_SIZE).map { row ->
        row.map { char -> char.digitToInt().takeIf { it != 0 } }
    }
    val trace = traceHumanSolve(givens)
    if (!trace.solved) error("Skeleton puzzle is not solvable with human techniques")
    val cells = givens.map { it.toMutableList() }
    trace.moves.filterIsInstance<SudokuPlacementMove>().forEach { move ->
        cells[move.targetRow][move.targetCol] = move.digit
    }
    return cells.map { row -> row.map { it ?: error("Skeleton puzzle left an empty cell") } }
}
