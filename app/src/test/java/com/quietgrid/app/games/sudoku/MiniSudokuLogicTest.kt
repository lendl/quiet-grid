package com.quietgrid.app.games.sudoku

import com.quietgrid.engine.sudoku.SudokuLayout
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniSudokuLogicTest {
    private val solution = listOf(
        listOf(1, 2, 3, 4, 5, 6),
        listOf(4, 5, 6, 1, 2, 3),
        listOf(2, 3, 1, 5, 6, 4),
        listOf(5, 6, 4, 2, 3, 1),
        listOf(3, 1, 2, 6, 4, 5),
        listOf(6, 4, 5, 3, 1, 2),
    )
    private val puzzle = SudokuPuzzleEntry(
        id = "m6-test",
        difficulty = "medium",
        givens = solution.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r == 0 && c < 3) null else v } },
        solution = solution,
    )

    @Test
    fun `a six by six session sizes notes and finished cells to the grid`() {
        val session = createSudokuSession(puzzle)
        assertEquals(6, session.notes.size)
        assertEquals(6, session.finishedCells[5].size)
    }

    @Test
    fun `touched units use two by three boxes`() {
        assertEquals(listOf("r3", "c4", "b3"), sudokuTouchedUnitKeys(3, 4, SudokuLayout.MINI_6))
    }

    @Test
    fun `finishing the first box marks only its cells`() {
        val session = createSudokuSession(puzzle)
        val board = solution
        val result = applySudokuFinalizeValidation(session, board, listOf("b0"))
        assertEquals(listOf(0), result.effect.correctBoxIndexes)
        assertTrue((0 until 3).all { result.session.finishedCells[0][it] })
        assertTrue(isSudokuSolved(board, solution))
    }

    @Test
    fun `auto notes only offer digits up to six`() {
        val notes = computeSudokuAutoNotes(puzzle.givens)
        assertEquals(setOf(1), notes[0][0])
        assertEquals(setOf(3), notes[0][2])
    }

    @Test
    fun `hint on a six by six board finds the next placement`() {
        val hint = getSudokuNextMoveHint(puzzle.givens) as SudokuPlacementHint
        assertTrue(hint.row == 0 && hint.col < 3)
        assertEquals(solution[hint.row][hint.col], hint.digit)
    }

    @Test
    fun `duplicate in a six by six box is reported as a conflict`() {
        val board = puzzle.givens.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r == 0 && c == 0) 4 else v } }
        val conflict = findSudokuInvalidState(board) as SudokuInvalidConflict
        assertEquals(4, conflict.digit)
    }
}
