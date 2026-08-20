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
        state.setCandidates(0, 2, 5)
        state.setCandidates(0, 7, 5)
        state.setCandidates(3, 2, 5)
        state.setCandidates(3, 7, 5)
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
        state.setCandidates(0, 1, 7)
        state.setCandidates(0, 4, 7)
        state.setCandidates(1, 4, 7)
        state.setCandidates(1, 7, 7)
        state.setCandidates(2, 1, 7)
        state.setCandidates(2, 7, 7)
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
        state.setCandidates(0, 0, 1, 2)
        state.setCandidates(0, 5, 1, 3)
        state.setCandidates(5, 0, 2, 3)
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
        state.setCandidates(0, 0, 1, 2, 3)
        state.setCandidates(0, 1, 1, 3)
        state.setCandidates(1, 0, 2, 3)
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
        state.setCandidates(0, 0, 1, 2)
        state.setCandidates(0, 3, 2, 3)
        state.setCandidates(5, 3, 3, 4)
        state.setCandidates(5, 7, 4, 1)
        state.setCandidates(5, 0, 1)

        val move = ChainsTechnique.findMove(state)
        assertNotNull(move)
        val elimination = move as SudokuCandidateEliminationMove
        assertEquals(SudokuTechnique.CHAINS, elimination.technique)
        assertEquals(listOf(Triple(5, 0, 1)), elimination.eliminations)
    }
}
