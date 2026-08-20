package com.quietgrid.app.games.sudoku

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

private val givensGrid: List<List<Int?>> = solutionGrid.mapIndexed { r, row ->
    row.mapIndexed { c, value -> if (r == 0 && (c == 0 || c == 1)) null else value }
}

private val puzzleEntry = SudokuPuzzleEntry(id = "p", difficulty = Difficulty.EASY.key, givens = givensGrid, solution = solutionGrid)

class SudokuLogicTest {

    @Test
    fun `createSudokuSession starts from the givens with empty notes`() {
        val session = createSudokuSession(puzzleEntry)

        assertEquals(givensGrid, session.board)
        assertTrue(session.notes.all { row -> row.all { it.isEmpty() } })
        assertEquals(SudokuInputMode.DIGIT, session.inputMode)
        assertEquals(0, session.accuracyDrops)
    }

    @Test
    fun `setDigit on a given cell is rejected`() {
        val session = createSudokuSession(puzzleEntry)

        assertNull(applySudokuSetDigit(session, row = 1, col = 0, digit = 9))
    }

    @Test
    fun `setDigit on a finished cell is rejected`() {
        val session = createSudokuSession(puzzleEntry).let { it.copy(finishedCells = it.finishedCells.mapIndexed { r, row -> row.mapIndexed { c, v -> v || (r == 0 && c == 0) } }) }

        assertNull(applySudokuSetDigit(session, row = 0, col = 0, digit = 5))
    }

    @Test
    fun `setDigit re-entering the same value is a no-op`() {
        val session = createSudokuSession(puzzleEntry).let { applySudokuSetDigit(it, 0, 0, 5)!! }

        assertNull(applySudokuSetDigit(session, row = 0, col = 0, digit = 5))
    }

    @Test
    fun `setDigit fills the cell and clears any notes there`() {
        val session = createSudokuSession(puzzleEntry).let { applySudokuToggleNote(it, 0, 0, 5)!! }

        val result = applySudokuSetDigit(session, row = 0, col = 0, digit = 7)!!

        assertEquals(7, result.board[0][0])
        assertTrue(result.notes[0][0].isEmpty())
    }

    @Test
    fun `clearCell empties a digit-filled cell`() {
        val session = createSudokuSession(puzzleEntry).let { applySudokuSetDigit(it, 0, 0, 7)!! }

        val result = applySudokuClearCell(session, row = 0, col = 0)!!

        assertNull(result.board[0][0])
    }

    @Test
    fun `clearCell on an already-empty cell with no notes is a no-op`() {
        val session = createSudokuSession(puzzleEntry)

        assertNull(applySudokuClearCell(session, row = 0, col = 0))
    }

    @Test
    fun `clearCell on a given cell is rejected`() {
        val session = createSudokuSession(puzzleEntry)

        assertNull(applySudokuClearCell(session, row = 1, col = 0))
    }

    @Test
    fun `toggleNote adds then removes a candidate digit`() {
        val session = createSudokuSession(puzzleEntry)

        val added = applySudokuToggleNote(session, row = 0, col = 0, digit = 4)!!
        assertEquals(setOf(4), added.notes[0][0])

        val removed = applySudokuToggleNote(added, row = 0, col = 0, digit = 4)!!
        assertTrue(removed.notes[0][0].isEmpty())
    }

    @Test
    fun `toggleNote on a cell that already has a digit is rejected`() {
        val session = createSudokuSession(puzzleEntry).let { applySudokuSetDigit(it, 0, 0, 7)!! }

        assertNull(applySudokuToggleNote(session, row = 0, col = 0, digit = 4))
    }

    @Test
    fun `sudokuScore is at its ceiling immediately and floors out for very slow or inaccurate play`() {
        assertEquals(10_000, sudokuScore(Difficulty.EASY, timeSeconds = 0, accuracyDrops = 0))
        assertEquals(1_000, sudokuScore(Difficulty.EASY, timeSeconds = 100_000, accuracyDrops = 0))
        assertEquals(1_000, sudokuScore(Difficulty.EASY, timeSeconds = 0, accuracyDrops = 100))
    }

    @Test
    fun `sudokuAccuracyPct drops by 10 per accuracy drop down to zero`() {
        assertEquals(100, sudokuAccuracyPct(0))
        assertEquals(70, sudokuAccuracyPct(3))
        assertEquals(0, sudokuAccuracyPct(20))
    }

    @Test
    fun `hasMeaningfulProgress is false for a fresh session`() {
        assertFalse(sudokuHasMeaningfulProgress(createSudokuSession(puzzleEntry)))
    }

    @Test
    fun `hasMeaningfulProgress is true once a digit is entered`() {
        val session = createSudokuSession(puzzleEntry).let { applySudokuSetDigit(it, 0, 0, 7)!! }

        assertTrue(sudokuHasMeaningfulProgress(session))
    }

    @Test
    fun `hasMeaningfulProgress is true once a note is added`() {
        val session = createSudokuSession(puzzleEntry).let { applySudokuToggleNote(it, 0, 0, 4)!! }

        assertTrue(sudokuHasMeaningfulProgress(session))
    }
}
