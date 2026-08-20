package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.sudoku.SudokuCandidateEliminationMove
import com.quietgrid.engine.sudoku.SudokuCellRef
import com.quietgrid.engine.sudoku.SudokuGrid
import com.quietgrid.engine.sudoku.SudokuHouseRef
import com.quietgrid.engine.sudoku.SudokuPlacementMove
import com.quietgrid.engine.sudoku.createBitmaskStateFromBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BasicTechniquesTest {
    private val solvedRow0 = intArrayOf(5, 3, 4, 6, 7, 8, 9, 1, 2)
    private val fullBoard: SudokuGrid = listOf(
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

    private fun gridOf(vararg cells: Triple<Int, Int, Int>): SudokuGrid {
        val rows = MutableList(9) { MutableList<Int?>(9) { null } }
        cells.forEach { (r, c, v) -> rows[r][c] = v }
        return rows
    }

    @Test
    fun `NakedSingleTechnique finds the single forced digit`() {
        val board = fullBoard.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r == 0 && c == 0) null else v } }
        val state = createBitmaskStateFromBoard(board)
        val move = NakedSingleTechnique.findMove(state)
        assertNotNull(move)
        val placement = move as SudokuPlacementMove
        assertEquals(0, placement.targetRow)
        assertEquals(0, placement.targetCol)
        assertEquals(5, placement.digit)
    }

    @Test
    fun `HiddenSingleTechnique is null on a board with no hidden singles`() {
        val state = createBitmaskStateFromBoard(fullBoard)
        assertNull(HiddenSingleTechnique.findMove(state))
    }

    @Test
    fun `HiddenSingleTechnique finds a digit confined to one cell that is not itself a naked single`() {
        val board = gridOf(
            Triple(0, 3, 1), Triple(0, 4, 2), Triple(0, 5, 4), Triple(0, 6, 6), Triple(0, 7, 7), Triple(0, 8, 8),
            Triple(3, 1, 5), Triple(6, 2, 5),
        )
        val state = createBitmaskStateFromBoard(board)
        val move = HiddenSingleTechnique.findMove(state)
        assertNotNull(move)
        val placement = move as SudokuPlacementMove
        assertEquals(0, placement.targetRow)
        assertEquals(0, placement.targetCol)
        assertEquals(5, placement.digit)
        assertEquals(3, placement.complexity)
        assertEquals(
            listOf(SudokuHouseRef("row", 0), SudokuHouseRef("column", 0), SudokuHouseRef("box", 0)),
            placement.houses,
        )
    }

    @Test
    fun `NakedPairTechnique finds a same-box pair and eliminates from a third cell`() {
        val board = gridOf(
            Triple(0, 2, 3), Triple(1, 1, 4), Triple(1, 2, 5), Triple(2, 0, 6), Triple(2, 1, 7), Triple(2, 2, 8),
            Triple(0, 5, 1),
        )
        val state = createBitmaskStateFromBoard(board)
        val move = NakedPairTechnique.findMove(state)
        assertNotNull(move)
        val elimination = move as SudokuCandidateEliminationMove
        assertEquals(3, elimination.complexity)
        assertEquals(listOf(SudokuHouseRef("box", 0)), elimination.houses)
        assertEquals(
            listOf(SudokuCellRef(0, 0), SudokuCellRef(0, 1)),
            elimination.evidenceCells,
        )
        assertEquals(
            listOf(Triple(1, 0, 2), Triple(1, 0, 9)),
            elimination.eliminations,
        )
    }

    @Test
    fun `HiddenPairTechnique finds two digits confined to the same two cells and strips their extra candidates`() {
        val board = gridOf(
            Triple(0, 3, 1), Triple(0, 4, 2), Triple(0, 5, 4), Triple(0, 6, 6), Triple(0, 7, 7), Triple(0, 8, 8),
            Triple(3, 2, 3), Triple(4, 2, 9),
        )
        val state = createBitmaskStateFromBoard(board)
        val move = HiddenPairTechnique.findMove(state)
        assertNotNull(move)
        val elimination = move as SudokuCandidateEliminationMove
        assertEquals(4, elimination.complexity)
        assertEquals(listOf(SudokuHouseRef("row", 0)), elimination.houses)
        assertEquals(
            listOf(SudokuCellRef(0, 0), SudokuCellRef(0, 1)),
            elimination.evidenceCells,
        )
        assertEquals(
            listOf(Triple(0, 0, 5), Triple(0, 1, 5)),
            elimination.eliminations,
        )
    }

    @Test
    fun `PointingPairTripleTechnique confines a digit to one row within a box and eliminates outside it`() {
        val board = gridOf(
            Triple(0, 2, 1),
            Triple(1, 0, 2), Triple(1, 1, 3), Triple(1, 2, 4),
            Triple(2, 0, 5), Triple(2, 1, 6), Triple(2, 2, 8),
        )
        val state = createBitmaskStateFromBoard(board)
        val move = PointingPairTripleTechnique.findMove(state)
        assertNotNull(move)
        val elimination = move as SudokuCandidateEliminationMove
        assertEquals(2, elimination.complexity)
        assertEquals(listOf(SudokuHouseRef("box", 0), SudokuHouseRef("row", 0)), elimination.houses)
        assertEquals(
            listOf(SudokuCellRef(0, 0), SudokuCellRef(0, 1)),
            elimination.evidenceCells,
        )
        assertEquals(
            listOf(
                Triple(0, 3, 7), Triple(0, 4, 7), Triple(0, 5, 7),
                Triple(0, 6, 7), Triple(0, 7, 7), Triple(0, 8, 7),
            ),
            elimination.eliminations,
        )
    }

    @Test
    fun `BoxLineReductionTechnique confines a digit to one box within a row and eliminates outside it`() {
        val board = gridOf(
            Triple(0, 2, 1), Triple(0, 3, 2), Triple(0, 4, 3), Triple(0, 5, 4), Triple(0, 6, 5), Triple(0, 7, 6), Triple(0, 8, 8),
        )
        val state = createBitmaskStateFromBoard(board)
        val move = BoxLineReductionTechnique.findMove(state)
        assertNotNull(move)
        val elimination = move as SudokuCandidateEliminationMove
        assertEquals(2, elimination.complexity)
        assertEquals(listOf(SudokuHouseRef("row", 0), SudokuHouseRef("box", 0)), elimination.houses)
        assertEquals(
            listOf(SudokuCellRef(0, 0), SudokuCellRef(0, 1)),
            elimination.evidenceCells,
        )
        assertEquals(
            listOf(
                Triple(1, 0, 7), Triple(1, 1, 7), Triple(1, 2, 7),
                Triple(2, 0, 7), Triple(2, 1, 7), Triple(2, 2, 7),
            ),
            elimination.eliminations,
        )
    }

    @Test
    fun `all six basic techniques are null on a fully solved board`() {
        val state = createBitmaskStateFromBoard(fullBoard)
        assertNull(NakedSingleTechnique.findMove(state))
        assertNull(HiddenSingleTechnique.findMove(state))
        assertNull(NakedPairTechnique.findMove(state))
        assertNull(HiddenPairTechnique.findMove(state))
        assertNull(PointingPairTripleTechnique.findMove(state))
        assertNull(BoxLineReductionTechnique.findMove(state))
    }
}
