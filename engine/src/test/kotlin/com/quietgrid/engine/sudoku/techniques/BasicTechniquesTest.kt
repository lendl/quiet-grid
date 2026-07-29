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
    // A valid solved board with cell (0,0) blanked -- every other cell in its row/col/box is
    // filled, so it has exactly one candidate: a naked single.
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

    // Builds a 9x9 grid of nulls with the given (row, col, digit) triples filled in -- lets
    // each fixture below spell out only the cells it cares about.
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
        assertNull(HiddenSingleTechnique.findMove(state)) // fully solved, nothing to find
    }

    @Test
    fun `HiddenSingleTechnique finds a digit confined to one cell that is not itself a naked single`() {
        // Row 0: (0,0),(0,1),(0,2) blank, rest filled with 1,2,4,6,7,8 -- row-missing digits
        // are {3,5,9}, so all three blanks start with that as their baseline candidate set.
        // Placing 5 elsewhere in columns 1 and 2 (outside box 0) strips digit 5 from (0,1) and
        // (0,2) only, leaving (0,0) as the *only* row-0 cell that can hold 5 -- a hidden single.
        // (0,0) itself keeps 3 candidates ({3,5,9}), so this is NOT also a naked single.
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
        assertEquals(3, placement.complexity) // 3 empty row-0 cells * 1.2 row weight = 3.6, truncated to 3
        assertEquals(
            listOf(SudokuHouseRef("row", 0), SudokuHouseRef("column", 0), SudokuHouseRef("box", 0)),
            placement.houses,
        )
    }

    @Test
    fun `NakedPairTechnique finds a same-box pair and eliminates from a third cell`() {
        // Box 0 filled cells {1,3,4,5,6,7,8} leave {2,9} missing from the box. (0,0) and (0,1)
        // are otherwise unconstrained by their row/column, so both land on candidate set {2,9}
        // -- a naked pair. (1,0) is also blank but its row/column don't block 2 or 9 either, so
        // it keeps all 3 remaining digits {2,4... } -- concretely {2,9} plus whatever the box
        // still misses beyond the pair, giving it a 3-candidate mask that still contains 2 and 9,
        // so the pair eliminates both from it.
        val board = gridOf(
            Triple(0, 2, 3), Triple(1, 1, 4), Triple(1, 2, 5), Triple(2, 0, 6), Triple(2, 1, 7), Triple(2, 2, 8),
            Triple(0, 5, 1), // removes digit 1 from row 0's blanks, leaving box-missing {2,9} at (0,0)/(0,1)
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
        // Row 0 has 3 blanks: (0,0),(0,1),(0,2); the other 6 cells are filled with {1,2,4,6,7,8},
        // leaving {3,5,9} as the row-missing set shared by all three blanks. Placing 3 and 9
        // elsewhere in column 2 (outside box 0) strips both from (0,2) only, leaving it with just
        // {5}, while (0,0)/(0,1) keep the full {3,5,9}. So digits 3 and 9 are each confined to
        // exactly (0,0) and (0,1) within row 0 -- a hidden pair -- and digit 5 (present at all
        // three cells) must be eliminated from the pair cells since it isn't part of the pair.
        val board = gridOf(
            Triple(0, 3, 1), Triple(0, 4, 2), Triple(0, 5, 4), Triple(0, 6, 6), Triple(0, 7, 7), Triple(0, 8, 8),
            Triple(3, 2, 3), Triple(4, 2, 9),
        )
        val state = createBitmaskStateFromBoard(board)
        val move = HiddenPairTechnique.findMove(state)
        assertNotNull(move)
        val elimination = move as SudokuCandidateEliminationMove
        assertEquals(4, elimination.complexity) // 3 empty row-0 cells * 1.5 = 4.5, truncated to 4
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
        // Box 0 filled cells {1,2,3,4,5,6,8} leave {7,9} as the box-missing digits, both landing
        // as candidates at (0,0) and (0,1) (rows 1-2 of the box are fully filled with other
        // digits). Since both cells sit in row 0, digit 7 is confined to row 0 within the box --
        // a pointing pair -- so it must be eliminated from every other blank row-0 cell (cols 3-8).
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
        // Row 0's 7 filled cells {1,2,3,4,5,6,8} leave {7,9} as the row-missing digits, both
        // landing as candidates at (0,0) and (0,1) -- the only blanks in row 0, and both inside
        // box 0. Since digit 7's row-0 candidates are confined to box 0, it must be eliminated
        // from the rest of box 0 (rows 1-2), which are otherwise fully blank and so still carry 7.
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
