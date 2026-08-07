package com.quietgrid.app.games.sudoku

import org.junit.Assert.assertEquals
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

private val blankBoard: List<List<Int?>> = List(9) { List(9) { null } }

class SudokuNextMoveTest {

    @Test
    fun `findSudokuInvalidState is null for an empty board`() {
        assertNull(findSudokuInvalidState(blankBoard))
    }

    @Test
    fun `findSudokuInvalidState flags a duplicate digit in the same row`() {
        val board = blankBoard.mapIndexed { r, row -> if (r != 0) row else row.toMutableList().also { it[0] = 5; it[2] = 5 } }

        val result = findSudokuInvalidState(board)

        assertTrue(result is SudokuInvalidConflict)
        val conflict = result as SudokuInvalidConflict
        assertEquals("row", conflict.house.kind)
        assertEquals(0, conflict.house.index)
        assertEquals(5, conflict.digit)
    }

    @Test
    fun `findSudokuInvalidState flags a cell with no legal digit left`() {
        // Row 0 uses digits 1-8 everywhere except (0,0); (1,0) supplies the missing 9, so (0,0)'s
        // row+column+box union already covers every digit and it can never legally be filled.
        val board = blankBoard.mapIndexed { r, row ->
            when (r) {
                0 -> listOf(null, 1, 2, 3, 4, 5, 6, 7, 8)
                1 -> listOf(9, null, null, null, null, null, null, null, null)
                else -> row
            }
        }

        val result = findSudokuInvalidState(board)

        assertTrue(result is SudokuInvalidDeadCell)
        val dead = result as SudokuInvalidDeadCell
        assertEquals(0, dead.row)
        assertEquals(0, dead.col)
    }

    @Test
    fun `getSudokuNextMoveHint is null once the board is solved`() {
        assertNull(getSudokuNextMoveHint(solutionGrid))
    }

    @Test
    fun `getSudokuNextMoveHint places the digit missing from an otherwise complete board`() {
        val almostSolved = solutionGrid.mapIndexed { r, row -> if (r == 0) row.mapIndexed { c, v -> if (c == 0) null else v } else row }

        val hint = getSudokuNextMoveHint(almostSolved)

        assertTrue(hint is SudokuPlacementHint)
        val placement = hint as SudokuPlacementHint
        assertEquals(0, placement.row)
        assertEquals(0, placement.col)
        assertEquals(5, placement.digit)
    }
}
