package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.sudoku.SudokuBitmaskState
import com.quietgrid.engine.sudoku.SudokuCandidateEliminationMove
import com.quietgrid.engine.sudoku.SudokuCellRef
import com.quietgrid.engine.sudoku.SudokuGrid
import com.quietgrid.engine.sudoku.SudokuTechnique
import com.quietgrid.engine.sudoku.createBitmaskStateFromBoard
import com.quietgrid.engine.sudoku.digitToBit
import com.quietgrid.engine.sudoku.getCellIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AdvancedTechniquesTest {
    private val fullySolved: SudokuGrid = listOf(
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

    @Test
    fun `all 6 advanced techniques find nothing on a fully solved board without crashing`() {
        val state = createBitmaskStateFromBoard(fullySolved)
        assertNull(XWingTechnique.findMove(state))
        assertNull(SwordfishTechnique.findMove(state))
        assertNull(XyWingTechnique.findMove(state))
        assertNull(XyzWingTechnique.findMove(state))
        assertNull(ColoringTechnique.findMove(state))
        assertNull(ChainsTechnique.findMove(state))
    }

    // --- Positive-case fixtures --------------------------------------------------------------
    //
    // Hand-constructing a *legal, fully consistent* 81-cell board that naturally produces one of
    // these patterns (while leaving every other cell's candidates irrelevant to the pattern) is
    // impractical -- these techniques only fire on boards deep enough into solving that most
    // cells are constrained by many other candidates too. But every dispatcher above only reads
    // state.board / state.candidateMask (never rowMask/colMask/boxMask/unresolvedCount), so we
    // can construct a minimal SudokuBitmaskState directly: every cell starts unsolved (board = 0)
    // with no candidates (mask = 0), and each test sets bits only on the handful of cells its
    // pattern needs. This isolates each technique's logic from puzzle-construction concerns.

    private fun blankState(): SudokuBitmaskState = SudokuBitmaskState(
        board = IntArray(81),
        candidateMask = IntArray(81),
        rowMask = IntArray(9),
        colMask = IntArray(9),
        boxMask = IntArray(9),
        unresolvedCount = 81,
    )

    private fun SudokuBitmaskState.setCandidates(row: Int, col: Int, vararg digits: Int) {
        candidateMask[getCellIndex(row, col)] = digits.fold(0) { mask, d -> mask or digitToBit[d] }
    }

    @Test
    fun `XWingTechnique finds a row-based x-wing and eliminates the shared columns elsewhere`() {
        val state = blankState()
        // Digit 5 confined to columns 2 and 7 in both row 0 and row 3 -- a row-based x-wing.
        state.setCandidates(0, 2, 5)
        state.setCandidates(0, 7, 5)
        state.setCandidates(3, 2, 5)
        state.setCandidates(3, 7, 5)
        // Stray candidate elsewhere in column 2 that the x-wing must eliminate.
        state.setCandidates(5, 2, 5)

        val move = XWingTechnique.findMove(state)
        assertNotNull(move)
        val elimination = move as SudokuCandidateEliminationMove
        assertEquals(SudokuTechnique.X_WING, elimination.technique)
        assertEquals(listOf(Triple(5, 2, 5)), elimination.eliminations)
        assertEquals(
            listOf(SudokuCellRef(0, 2), SudokuCellRef(0, 7), SudokuCellRef(3, 2), SudokuCellRef(3, 7)),
            elimination.evidenceCells,
        )
    }

    @Test
    fun `SwordfishTechnique finds a row-based swordfish and eliminates the shared columns elsewhere`() {
        val state = blankState()
        // Digit 7: row 0 -> cols {1,4}; row 1 -> cols {4,7}; row 2 -> cols {1,7}; column union
        // across all three rows is exactly {1,4,7} -- a row-based swordfish.
        state.setCandidates(0, 1, 7)
        state.setCandidates(0, 4, 7)
        state.setCandidates(1, 4, 7)
        state.setCandidates(1, 7, 7)
        state.setCandidates(2, 1, 7)
        state.setCandidates(2, 7, 7)
        // Stray candidate in column 1 outside the fish rows that must be eliminated.
        state.setCandidates(5, 1, 7)

        val move = SwordfishTechnique.findMove(state)
        assertNotNull(move)
        val elimination = move as SudokuCandidateEliminationMove
        assertEquals(SudokuTechnique.SWORDFISH, elimination.technique)
        assertEquals(listOf(Triple(5, 1, 7)), elimination.eliminations)
    }

    @Test
    fun `XyWingTechnique finds a pivot-and-two-wings pattern and eliminates the shared z digit`() {
        val state = blankState()
        state.setCandidates(0, 0, 1, 2) // pivot {1,2}
        state.setCandidates(0, 5, 1, 3) // left wing {1,3}, row-peer of pivot
        state.setCandidates(5, 0, 2, 3) // right wing {2,3}, column-peer of pivot
        // (5,5) is a peer of both wings (column 5 and row 5) and carries the shared z digit 3.
        state.setCandidates(5, 5, 3)

        val move = XyWingTechnique.findMove(state)
        assertNotNull(move)
        val elimination = move as SudokuCandidateEliminationMove
        assertEquals(SudokuTechnique.XY_WING, elimination.technique)
        assertEquals(listOf(Triple(5, 5, 3)), elimination.eliminations)
        assertEquals(
            listOf(SudokuCellRef(0, 0), SudokuCellRef(0, 5), SudokuCellRef(5, 0)),
            elimination.evidenceCells,
        )
    }

    @Test
    fun `XyzWingTechnique finds a tri-value pivot with two bivalue wings and eliminates the shared z digit`() {
        val state = blankState()
        state.setCandidates(0, 0, 1, 2, 3) // pivot {1,2,3}
        state.setCandidates(0, 1, 1, 3) // left wing {1,3}, row+box peer of pivot
        state.setCandidates(1, 0, 2, 3) // right wing {2,3}, column+box peer of pivot
        // (2,2) shares box 0 with all three cells and carries the shared z digit 3.
        state.setCandidates(2, 2, 3)

        val move = XyzWingTechnique.findMove(state)
        assertNotNull(move)
        val elimination = move as SudokuCandidateEliminationMove
        assertEquals(SudokuTechnique.XYZ_WING, elimination.technique)
        assertEquals(listOf(Triple(2, 2, 3)), elimination.eliminations)
    }

    @Test
    fun `ColoringTechnique finds two same-colored peers and eliminates the digit from both`() {
        val state = blankState()
        // Digit 4: row 0 links (0,0)-(0,1); column 1 links (0,1)-(2,1). BFS colors (0,0) and
        // (2,1) the same color, and they happen to also share box 0 -- a same-color conflict,
        // meaning that color's assumption is impossible and the digit must be eliminated from
        // every cell colored that way.
        state.setCandidates(0, 0, 4)
        state.setCandidates(0, 1, 4)
        state.setCandidates(2, 1, 4)

        val move = ColoringTechnique.findMove(state)
        assertNotNull(move)
        val elimination = move as SudokuCandidateEliminationMove
        assertEquals(SudokuTechnique.COLORING, elimination.technique)
        assertEquals(listOf(Triple(0, 0, 4), Triple(2, 1, 4)), elimination.eliminations)
    }

    @Test
    fun `ChainsTechnique follows a 4-cell bivalue chain back to the start digit and eliminates elsewhere`() {
        val state = blankState()
        // Alternating bivalue chain: (0,0){1,2} --2--> (0,3){2,3} --3--> (5,3){3,4} --4-->
        // (5,7){4,1}, which closes the loop back to digit 1 (the chain's original target digit).
        state.setCandidates(0, 0, 1, 2)
        state.setCandidates(0, 3, 2, 3)
        state.setCandidates(5, 3, 3, 4)
        state.setCandidates(5, 7, 4, 1)
        // (5,0) is a peer of both chain endpoints (0,0) and (5,7) and carries digit 1.
        state.setCandidates(5, 0, 1)

        val move = ChainsTechnique.findMove(state)
        assertNotNull(move)
        val elimination = move as SudokuCandidateEliminationMove
        assertEquals(SudokuTechnique.CHAINS, elimination.technique)
        assertEquals(listOf(Triple(5, 0, 1)), elimination.eliminations)
    }
}
