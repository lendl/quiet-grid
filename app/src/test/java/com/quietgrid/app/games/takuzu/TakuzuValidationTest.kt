package com.quietgrid.app.games.takuzu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val solutionGrid = listOf(
    listOf(0, 1, 0, 1),
    listOf(1, 0, 1, 0),
    listOf(0, 1, 1, 0),
    listOf(1, 0, 0, 1),
)

class TakuzuValidationTest {

    @Test
    fun `getCompletedLineStateForKey is INCOMPLETE while any cell in the line is blank`() {
        val board = solutionGrid.mapIndexed { r, row -> if (r != 0) row else row.mapIndexed { c, v -> if (c == 0) null else v } }

        assertEquals(CompletedLineState.INCOMPLETE, getCompletedLineStateForKey(board, solutionGrid, "r0"))
    }

    @Test
    fun `getCompletedLineStateForKey is CORRECT for a completed line matching the solution`() {
        assertEquals(CompletedLineState.CORRECT, getCompletedLineStateForKey(solutionGrid, solutionGrid, "r0"))
        assertEquals(CompletedLineState.CORRECT, getCompletedLineStateForKey(solutionGrid, solutionGrid, "c1"))
    }

    @Test
    fun `getCompletedLineStateForKey is INCORRECT for a completed line mismatching the solution`() {
        val board = solutionGrid.mapIndexed { r, row -> if (r != 0) row else listOf(1, 0, 0, 1) }

        assertEquals(CompletedLineState.INCORRECT, getCompletedLineStateForKey(board, solutionGrid, "r0"))
    }

    @Test
    fun `getTouchedLineStates reports the row and column state for a cell`() {
        val board = solutionGrid.mapIndexed { r, row -> if (r != 0) row else listOf(1, 0, 0, 1) }

        val touched = getTouchedLineStates(board, solutionGrid, row = 0, col = 0)

        assertEquals(CompletedLineState.INCORRECT, touched.rowState)
        assertEquals(CompletedLineState.INCORRECT, touched.colState)
    }

    @Test
    fun `getMismatchedCompletedLines lists every incorrect completed row and column`() {
        val board = solutionGrid.mapIndexed { r, row -> if (r != 0) row else listOf(1, 0, 0, 1) }

        val mismatched = getMismatchedCompletedLines(board, solutionGrid)

        assertTrue("r0" in mismatched)
        assertTrue("c0" in mismatched)
        assertTrue("c1" in mismatched)
        assertTrue("c2" !in mismatched)
        assertTrue("c3" !in mismatched)
    }

    @Test
    fun `isBoardSolved is true only when the board exactly matches the solution`() {
        assertTrue(isBoardSolved(solutionGrid, solutionGrid))

        val almost = solutionGrid.mapIndexed { r, row -> if (r != 0) row else row.mapIndexed { c, v -> if (c == 0) null else v } }
        assertEquals(false, isBoardSolved(almost, solutionGrid))
    }
}
