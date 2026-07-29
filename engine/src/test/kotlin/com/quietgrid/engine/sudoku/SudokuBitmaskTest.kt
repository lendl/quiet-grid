package com.quietgrid.engine.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Classic valid, fully-solved 9x9 Sudoku grid used by multiple tests below. */
private val SOLVED_SUDOKU_ROWS: List<List<Int>> = listOf(
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

private val SOLVED_SUDOKU_GRID: SudokuGrid = SOLVED_SUDOKU_ROWS.map { row -> row.map<Int, SudokuCellValue> { it } }

class SudokuBitmaskTest {
    @Test
    fun `createBitmaskStateFromBoard computes candidate masks that exclude row column and box peers`() {
        val board: SudokuGrid = List(9) { r -> List(9) { c -> if (r == 0 && c == 0) 5 else null } }
        val state = createBitmaskStateFromBoard(board)
        // Row 0 peers, col 0 peers, and box 0 peers must not offer digit 5 as a candidate.
        assertEquals(0, state.candidateMask[getCellIndex(0, 4)] and digitToBit[5])
        assertEquals(0, state.candidateMask[getCellIndex(4, 0)] and digitToBit[5])
        assertEquals(0, state.candidateMask[getCellIndex(1, 1)] and digitToBit[5])
        assertTrue((state.candidateMask[getCellIndex(4, 4)] and digitToBit[5]) != 0)
    }

    @Test
    fun `placeDigit updates masks and clears peer candidates`() {
        val board: SudokuGrid = List(9) { List(9) { null } }
        val state = createBitmaskStateFromBoard(board)
        placeDigit(state, getCellIndex(0, 0), 7)
        assertEquals(0, state.candidateMask[getCellIndex(0, 1)] and digitToBit[7])
        assertEquals(80, state.unresolvedCount)
    }

    @Test
    fun `cellPeers for a corner cell has 20 entries (8 row + 8 col + 4 remaining box)`() {
        assertEquals(20, cellPeers[getCellIndex(0, 0)].size)
    }

    // --- cloneBitmaskState independence -------------------------------------------------

    @Test
    fun `cloneBitmaskState board mutation on clone does not affect original`() {
        val board: SudokuGrid = List(9) { List(9) { null } }
        val original = createBitmaskStateFromBoard(board)
        val clone = cloneBitmaskState(original)

        clone.board[getCellIndex(0, 0)] = 9

        assertEquals(9, clone.board[getCellIndex(0, 0)])
        assertEquals(0, original.board[getCellIndex(0, 0)])
    }

    @Test
    fun `cloneBitmaskState board mutation on original does not affect clone`() {
        val board: SudokuGrid = List(9) { List(9) { null } }
        val original = createBitmaskStateFromBoard(board)
        val clone = cloneBitmaskState(original)

        original.board[getCellIndex(0, 0)] = 4

        assertEquals(4, original.board[getCellIndex(0, 0)])
        assertEquals(0, clone.board[getCellIndex(0, 0)])
    }

    @Test
    fun `cloneBitmaskState candidateMask mutation on clone does not affect original`() {
        val board: SudokuGrid = List(9) { List(9) { null } }
        val original = createBitmaskStateFromBoard(board)
        val clone = cloneBitmaskState(original)
        val index = getCellIndex(3, 3)
        val originalMaskBefore = original.candidateMask[index]

        clone.candidateMask[index] = 0

        assertEquals(0, clone.candidateMask[index])
        assertEquals(originalMaskBefore, original.candidateMask[index])
        assertNotEquals(clone.candidateMask[index], original.candidateMask[index])
    }

    @Test
    fun `cloneBitmaskState rowMask colMask boxMask and unresolvedCount are independently copied`() {
        val board: SudokuGrid = List(9) { r -> List(9) { c -> if (r == 0 && c == 0) 5 else null } }
        val original = createBitmaskStateFromBoard(board)
        val clone = cloneBitmaskState(original)

        // Mutating the clone's row/col/box masks and unresolvedCount must not leak back to original.
        clone.rowMask[0] = FULL_MASK
        clone.colMask[0] = FULL_MASK
        clone.boxMask[0] = FULL_MASK
        clone.unresolvedCount = -1

        assertEquals(FULL_MASK, clone.rowMask[0])
        assertEquals(FULL_MASK, clone.colMask[0])
        assertEquals(FULL_MASK, clone.boxMask[0])
        assertEquals(-1, clone.unresolvedCount)

        assertNotEquals(FULL_MASK, original.rowMask[0])
        assertNotEquals(FULL_MASK, original.colMask[0])
        assertNotEquals(FULL_MASK, original.boxMask[0])
        assertEquals(80, original.unresolvedCount)
    }

    // --- duplicate-digit guard -----------------------------------------------------------

    @Test
    fun `createBitmaskStateFromBoard throws when the same digit appears twice in a row`() {
        val board: SudokuGrid = List(9) { r ->
            List(9) { c -> if (r == 0 && (c == 0 || c == 1)) 5 else null }
        }
        assertThrows(IllegalStateException::class.java) {
            createBitmaskStateFromBoard(board)
        }
    }

    @Test
    fun `createBitmaskStateFromBoard throws when the same digit appears twice in a column`() {
        val board: SudokuGrid = List(9) { r ->
            List(9) { c -> if (c == 0 && (r == 0 || r == 1)) 5 else null }
        }
        assertThrows(IllegalStateException::class.java) {
            createBitmaskStateFromBoard(board)
        }
    }

    @Test
    fun `createBitmaskStateFromBoard throws when the same digit appears twice in a box`() {
        // (0,0) and (1,1) share box 0 but not a row or column.
        val board: SudokuGrid = List(9) { r ->
            List(9) { c -> if ((r == 0 && c == 0) || (r == 1 && c == 1)) 5 else null }
        }
        assertThrows(IllegalStateException::class.java) {
            createBitmaskStateFromBoard(board)
        }
    }

    @Test
    fun `createBitmaskStateFromFlatBoard throws when the same digit appears twice in a row`() {
        val flatBoard = IntArray(81)
        flatBoard[getCellIndex(0, 0)] = 3
        flatBoard[getCellIndex(0, 5)] = 3
        assertThrows(IllegalStateException::class.java) {
            createBitmaskStateFromFlatBoard(flatBoard)
        }
    }

    // --- eliminateCandidate ----------------------------------------------------------------

    @Test
    fun `eliminateCandidate removes exactly the targeted bit and leaves other bits untouched`() {
        val board: SudokuGrid = List(9) { List(9) { null } }
        val state = createBitmaskStateFromBoard(board)
        val index = getCellIndex(4, 4)
        val maskBefore = state.candidateMask[index]
        assertTrue((maskBefore and digitToBit[3]) != 0)
        assertTrue((maskBefore and digitToBit[7]) != 0)

        eliminateCandidate(state, index, 3)

        assertEquals(0, state.candidateMask[index] and digitToBit[3])
        // Every other candidate bit that was present before remains present.
        assertEquals(maskBefore and digitToBit[3].inv(), state.candidateMask[index])
        assertTrue((state.candidateMask[index] and digitToBit[7]) != 0)
    }

    @Test
    fun `eliminateCandidate is a no-op on an already-filled cell`() {
        val board: SudokuGrid = List(9) { List(9) { null } }
        val state = createBitmaskStateFromBoard(board)
        val index = getCellIndex(0, 0)
        placeDigit(state, index, 6)
        // Placing a digit clears the candidate mask for that cell; force it to a non-zero
        // sentinel value to prove eliminateCandidate's guard checks board state, not mask state.
        state.candidateMask[index] = FULL_MASK

        eliminateCandidate(state, index, 6)

        assertEquals(FULL_MASK, state.candidateMask[index])
        assertEquals(6, state.board[index])
    }

    // --- isSolved ----------------------------------------------------------------------------

    @Test
    fun `isSolved returns false for a partially-filled board`() {
        val board: SudokuGrid = List(9) { r -> List(9) { c -> if (r == 0 && c == 0) 5 else null } }
        val state = createBitmaskStateFromBoard(board)
        assertFalse(isSolved(state))
    }

    @Test
    fun `isSolved returns true for a fully-filled valid board`() {
        val state = createBitmaskStateFromBoard(SOLVED_SUDOKU_GRID)
        assertTrue(isSolved(state))
        assertEquals(0, state.unresolvedCount)
    }

    // --- cellPeers for a non-corner cell -------------------------------------------------

    @Test
    fun `cellPeers for the center cell has 20 entries and differs from the corner cell's peer set`() {
        val centerIndex = getCellIndex(4, 4)
        val cornerIndex = getCellIndex(0, 0)

        val centerPeers = cellPeers[centerIndex]
        val cornerPeers = cellPeers[cornerIndex]

        assertEquals(20, centerPeers.size)
        assertFalse(centerPeers.contains(centerIndex))
        // Center cell's row/col/box peers must be the ones actually sharing its row, column, or box.
        assertTrue(centerPeers.contains(getCellIndex(4, 0)))
        assertTrue(centerPeers.contains(getCellIndex(0, 4)))
        assertTrue(centerPeers.contains(getCellIndex(3, 3)))
        assertFalse(centerPeers.contains(getCellIndex(0, 0)))

        assertNotEquals(cornerPeers, centerPeers)
    }
}
