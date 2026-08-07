package com.quietgrid.app.games.nonogram

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A 2x2 puzzle solved as a checkerboard: (0,0) and (1,1) filled, the rest empty. */
private val entry = NonogramPuzzleEntry(
    id = "p",
    difficulty = Difficulty.EASY.key,
    rows = 2,
    cols = 2,
    solution = listOf(listOf(true, false), listOf(false, true)),
)

class NonogramLogicTest {

    @Test
    fun `buildNonogramPuzzle derives row and column clues from the solution`() {
        val puzzle = buildNonogramPuzzle(entry)

        assertEquals(listOf(listOf(1), listOf(1)), puzzle.rowClues)
        assertEquals(listOf(listOf(1), listOf(1)), puzzle.colClues)
    }

    @Test
    fun `createNonogramSession starts with an empty board`() {
        val session = createNonogramSession(entry)

        assertTrue(session.board.all { row -> row.all { it == null } })
        assertEquals(entry.solution, session.solution)
    }

    @Test
    fun `tapping in FILL mode sets a blank cell then clears it back to blank`() {
        val session = createNonogramSession(entry)

        val filled = applyNonogramTap(session, row = 0, col = 0, mode = NonogramInputMode.FILL)!!
        assertEquals(1, filled.board[0][0])

        val cleared = applyNonogramTap(filled, row = 0, col = 0, mode = NonogramInputMode.FILL)!!
        assertNull(cleared.board[0][0])
    }

    @Test
    fun `tapping in CROSS mode sets a blank cell to crossed then clears it back to blank`() {
        val session = createNonogramSession(entry)

        val crossed = applyNonogramTap(session, row = 0, col = 1, mode = NonogramInputMode.CROSS)!!
        assertEquals(0, crossed.board[0][1])

        val cleared = applyNonogramTap(crossed, row = 0, col = 1, mode = NonogramInputMode.CROSS)!!
        assertNull(cleared.board[0][1])
    }

    @Test
    fun `completing a row correctly auto-fills its remaining blanks as crossed`() {
        // Row 0's clue is [1]; filling (0,0) correctly completes the row, so (0,1) auto-crosses.
        val session = createNonogramSession(entry)

        val result = applyNonogramTap(session, row = 0, col = 0, mode = NonogramInputMode.FILL)!!

        assertEquals(1, result.board[0][0])
        assertEquals(0, result.board[0][1])
    }

    @Test
    fun `paint sets every listed cell to the given value`() {
        val session = createNonogramSession(entry)

        val result = applyNonogramPaint(session, cells = listOf(0 to 0, 0 to 1), value = 1)!!

        assertEquals(1, result.board[0][0])
        assertEquals(1, result.board[0][1])
    }

    @Test
    fun `paint is a no-op when every listed cell already has the target value`() {
        val session = createNonogramSession(entry)
        val painted = applyNonogramPaint(session, cells = listOf(0 to 0), value = 1)!!

        assertNull(applyNonogramPaint(painted, cells = listOf(0 to 0), value = 1))
    }

    @Test
    fun `isNonogramSolved requires filled cells to exactly match the solution`() {
        assertTrue(isNonogramSolved(entry.solution.map { row -> row.map { if (it) 1 else 0 } }, entry.solution))

        val wrong = listOf(listOf(0, 0), listOf(0, 1))
        assertFalse(isNonogramSolved(wrong, entry.solution))
    }

    @Test
    fun `hasMeaningfulProgress is false for a fresh board and true after any mark`() {
        val session = createNonogramSession(entry)
        assertFalse(nonogramHasMeaningfulProgress(session))

        val marked = applyNonogramTap(session, row = 0, col = 0, mode = NonogramInputMode.FILL)!!
        assertTrue(nonogramHasMeaningfulProgress(marked))
    }

    @Test
    fun `nonogramScore decreases with elapsed time and floors out`() {
        assertEquals(10_000, nonogramScore(0))
        assertTrue(nonogramScore(10) < nonogramScore(0))
        assertEquals(1_000, nonogramScore(10_000))
    }
}
