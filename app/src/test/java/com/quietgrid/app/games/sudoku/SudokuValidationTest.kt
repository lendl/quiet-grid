package com.quietgrid.app.games.sudoku

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val solutionGrid = listOf(
    listOf(5, 3, 4, 6, 7, 8, 9, 1, 2),
    listOf(6, 7, 2, 1, 9, 5, 3, 4, 8),
    listOf(1, 9, 8, 3, 4, 2, 5, 6, 7),
    listOf(8, 5, 9, 7, 6, 1, 4, 2, 3),
    listOf(4, 2, 6, 8, 5, 3, 7, 9, 1),
    listOf(7, 1, 3, 9, 2, 4, 8, 5, 6),
    listOf(9, 6, 1, 5, 3, 7, 2, 8, 4),
    listOf(2, 8, 7, 4, 1, 9, 6, 3, 5),
    listOf(3, 4, 5, 2, 8, 6, 1, 7, 9),
)

class SudokuValidationTest {

    @Test
    fun `sudokuBoxIndex maps row-col into the 0-8 box grid`() {
        assertEquals(0, sudokuBoxIndex(0, 0))
        assertEquals(0, sudokuBoxIndex(2, 2))
        assertEquals(4, sudokuBoxIndex(4, 4))
        assertEquals(8, sudokuBoxIndex(8, 8))
    }

    @Test
    fun `sudokuTouchedUnitKeys names the row, column, and box for a cell`() {
        assertEquals(listOf("r3", "c5", "b4"), sudokuTouchedUnitKeys(3, 5))
    }

    @Test
    fun `getCompletedSudokuUnitState is INCOMPLETE while any cell in the unit is blank`() {
        val board = solutionGrid.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r == 0 && c == 0) null else v } }

        assertEquals(SudokuUnitState.INCOMPLETE, getCompletedSudokuUnitState(board, solutionGrid, "r0"))
    }

    @Test
    fun `getCompletedSudokuUnitState is CORRECT when a completed unit matches the solution`() {
        assertEquals(SudokuUnitState.CORRECT, getCompletedSudokuUnitState(solutionGrid, solutionGrid, "r0"))
        assertEquals(SudokuUnitState.CORRECT, getCompletedSudokuUnitState(solutionGrid, solutionGrid, "c0"))
        assertEquals(SudokuUnitState.CORRECT, getCompletedSudokuUnitState(solutionGrid, solutionGrid, "b0"))
    }

    @Test
    fun `getCompletedSudokuUnitState is INCORRECT when a completed row mismatches the solution`() {
        val board = solutionGrid.mapIndexed { r, row -> if (r != 0) row else listOf(3, 5, 4, 6, 7, 8, 9, 1, 2) }

        assertEquals(SudokuUnitState.INCORRECT, getCompletedSudokuUnitState(board, solutionGrid, "r0"))
    }

    @Test
    fun `getCorrectSudokuUnitKeys and getMismatchedSudokuUnitKeys partition completed units`() {
        val board = solutionGrid.mapIndexed { r, row -> if (r != 0) row else listOf(3, 5, 4, 6, 7, 8, 9, 1, 2) }

        assertTrue("c2" in getCorrectSudokuUnitKeys(board, solutionGrid))
        assertTrue("r0" in getMismatchedSudokuUnitKeys(board, solutionGrid))
        assertTrue("c1" in getMismatchedSudokuUnitKeys(board, solutionGrid))
        assertTrue("r0" !in getCorrectSudokuUnitKeys(board, solutionGrid))
    }

    @Test
    fun `isSudokuSolved is true only when the board exactly matches the solution`() {
        assertTrue(isSudokuSolved(solutionGrid, solutionGrid))

        val almost = solutionGrid.mapIndexed { r, row -> if (r != 0) row else row.mapIndexed { c, v -> if (c == 0) null else v } }
        assertEquals(false, isSudokuSolved(almost, solutionGrid))
    }

    @Test
    fun `applySudokuFinalizeValidation reports a correct row and marks its blank-turned-filled cells finished`() {
        val puzzle = SudokuPuzzleEntry(
            id = "p",
            difficulty = Difficulty.EASY.key,
            givens = solutionGrid.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r == 0 && c == 0) null else v } },
            solution = solutionGrid,
        )
        val session = createSudokuSession(puzzle)
        val filledBoard = solutionGrid

        val result = applySudokuFinalizeValidation(session, filledBoard, listOf("r0"))

        assertEquals(listOf(0), result.effect.correctRowIndexes)
        assertTrue(result.effect.incorrectRowIndexes.isEmpty())
        assertTrue(result.session.finishedCells[0][0])
    }

    @Test
    fun `applySudokuFinalizeValidation reports an incorrect row and increments accuracyDrops once`() {
        val puzzle = SudokuPuzzleEntry(id = "p", difficulty = Difficulty.EASY.key, givens = solutionGrid, solution = solutionGrid)
        val session = createSudokuSession(puzzle)
        val wrongRow = solutionGrid.mapIndexed { r, row -> if (r != 0) row else listOf(3, 5, 4, 6, 7, 8, 9, 1, 2) }

        val result = applySudokuFinalizeValidation(session, wrongRow, listOf("r0"))

        assertEquals(listOf(0), result.effect.incorrectRowIndexes)
        assertEquals(1, result.session.accuracyDrops)

        val again = applySudokuFinalizeValidation(result.session, wrongRow, listOf("r0"))
        assertEquals(1, again.session.accuracyDrops)
    }
}
