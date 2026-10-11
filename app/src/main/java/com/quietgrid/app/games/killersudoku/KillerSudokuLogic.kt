package com.quietgrid.app.games.killersudoku

import com.quietgrid.app.games.sudoku.createSudokuSession
import com.quietgrid.engine.killersudoku.KillerCageLayout
import com.quietgrid.engine.killersudoku.KillerSudokuPuzzleEntry
import com.quietgrid.engine.killersudoku.killerBasicCandidateMasks
import com.quietgrid.engine.sudoku.SudokuGrid
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import com.quietgrid.engine.sudoku.iterateMaskDigits

fun killerGridEntry(entry: KillerSudokuPuzzleEntry): SudokuPuzzleEntry = SudokuPuzzleEntry(
    id = entry.id,
    difficulty = entry.difficulty,
    givens = List(9) { List(9) { null } },
    solution = entry.solution,
)

fun createKillerSudokuSession(entry: KillerSudokuPuzzleEntry): KillerSudokuSession =
    KillerSudokuSession(puzzle = entry, grid = createSudokuSession(killerGridEntry(entry)))

fun killerBoardArray(board: SudokuGrid): IntArray = IntArray(81) { board[it / 9][it % 9] ?: 0 }

fun computeKillerAutoNotes(layout: KillerCageLayout, board: SudokuGrid): List<List<Set<Int>>> {
    val masks = runCatching { killerBasicCandidateMasks(layout, killerBoardArray(board)) }.getOrNull()
        ?: return List(9) { List(9) { emptySet() } }
    return List(9) { row -> List(9) { col -> iterateMaskDigits(masks[row * 9 + col]).toSet() } }
}

fun killerCageLabelCells(layout: KillerCageLayout): Map<Int, Int> =
    layout.cages.associate { cage -> cage.cells.min() to cage.sum }
