package com.quietgrid.engine.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuTechniqueHelpersTest {
    @Test
    fun `technique rank order matches declaration order`() {
        assertTrue(compareTechniques(SudokuTechnique.NAKED_SINGLE, SudokuTechnique.CHAINS) < 0)
        assertEquals(SudokuTechnique.CHAINS, getHardestTechnique(listOf(SudokuTechnique.NAKED_SINGLE, SudokuTechnique.CHAINS, SudokuTechnique.X_WING)))
    }

    @Test
    fun `buildCandidateEliminationMove returns null when there are no eliminations`() {
        val move = buildCandidateEliminationMove(SudokuTechnique.NAKED_PAIR, emptyList(), listOf(0, 1), null, complexity = 0)
        assertNull(move)
    }

    @Test
    fun `arePeerIndexes is true for two cells in the same row`() {
        assertTrue(arePeerIndexes(getCellIndex(0, 0), getCellIndex(0, 8)))
    }

    // --- buildCandidateEliminationMove: dedup + sort ---------------------------------------

    @Test
    fun `buildCandidateEliminationMove dedups identical eliminations and sorts by row then col then digit`() {
        // Deliberately scrambled input order, plus a literal duplicate of the (5,2,7) triple,
        // so a broken sort or broken dedup would each independently fail this test.
        val eliminations = listOf(
            getCellIndex(5, 2) to 7,
            getCellIndex(1, 8) to 2,
            getCellIndex(0, 3) to 9,
            getCellIndex(1, 8) to 1,
            getCellIndex(5, 2) to 7, // duplicate of the first entry
        )

        val move = buildCandidateEliminationMove(
            SudokuTechnique.NAKED_PAIR,
            eliminations,
            evidenceCells = listOf(getCellIndex(0, 3)),
            houses = emptyList(),
            complexity = 3,
        )

        checkNotNull(move)
        // 5 inputs, 1 exact duplicate -> 4 unique eliminations.
        assertEquals(4, move.eliminations.size)
        assertEquals(
            listOf(
                Triple(0, 3, 9),
                Triple(1, 8, 1),
                Triple(1, 8, 2),
                Triple(5, 2, 7),
            ),
            move.eliminations,
        )
    }

    // --- buildPlacementMove: evidence-cell sort ---------------------------------------------

    @Test
    fun `buildPlacementMove sorts evidenceCells by row then col`() {
        val scrambledIndexes = listOf(getCellIndex(5, 5), getCellIndex(0, 8), getCellIndex(0, 2))

        val move = buildPlacementMove(
            SudokuTechnique.NAKED_SINGLE,
            row = 1,
            col = 1,
            digit = 4,
            evidenceCells = scrambledIndexes,
            houses = emptyList(),
            complexity = 0,
        )

        assertEquals(
            listOf(SudokuCellRef(0, 2), SudokuCellRef(0, 8), SudokuCellRef(5, 5)),
            move.evidenceCells,
        )
    }

    // --- collectHousesFromIndexes: house dedup + kind/index sort ---------------------------

    @Test
    fun `collectHousesFromIndexes dedups shared houses and sorts by kind then index`() {
        // (0,0) and (0,3) share row 0; (0,0) and (3,0) share column 0 -> both are duplicate
        // houses that must collapse to a single entry each.
        val indexes = listOf(getCellIndex(0, 0), getCellIndex(0, 3), getCellIndex(3, 0))

        val houses = collectHousesFromIndexes(indexes)

        // 3 indexes x 3 house kinds = 9 raw contributions, minus 2 duplicates (row 0, column 0) = 7 unique houses.
        assertEquals(7, houses.size)
        // "box" < "column" < "row" alphabetically -- verify that's the actual sort order produced.
        assertEquals(
            listOf(
                SudokuHouseRef("box", 0),
                SudokuHouseRef("box", 1),
                SudokuHouseRef("box", 3),
                SudokuHouseRef("column", 0),
                SudokuHouseRef("column", 3),
                SudokuHouseRef("row", 0),
                SudokuHouseRef("row", 3),
            ),
            houses,
        )
    }

    // --- peerIntersectionIndexes: real intersection -----------------------------------------

    @Test
    fun `peerIntersectionIndexes returns exactly the shared peers of both cells`() {
        // (4,4) and (4,0) share row 4. Their peer-set intersection is exactly the other row-4
        // cells: col/box-only peers of one are not peers of the other, and the two input cells
        // themselves are excluded (a cell is never its own peer).
        val result = peerIntersectionIndexes(listOf(getCellIndex(4, 4), getCellIndex(4, 0)))

        val expected = setOf(
            getCellIndex(4, 1), getCellIndex(4, 2), getCellIndex(4, 3),
            getCellIndex(4, 5), getCellIndex(4, 6), getCellIndex(4, 7), getCellIndex(4, 8),
        )
        assertEquals(expected, result.toSet())

        // Spot-check cells that are peers of only one of the two inputs -- must NOT appear.
        assertFalse(result.contains(getCellIndex(0, 4))) // column-4 peer of (4,4) only
        assertFalse(result.contains(getCellIndex(3, 1))) // box-3 peer of (4,0) only
        // The input cells themselves must never appear in their own intersection.
        assertFalse(result.contains(getCellIndex(4, 4)))
        assertFalse(result.contains(getCellIndex(4, 0)))
    }

    // --- getHouseDigitMatches ----------------------------------------------------------------

    @Test
    fun `getHouseDigitMatches returns exactly the house cells that still carry the digit as a candidate`() {
        // Placing 4 at (2,2) removes digit 4 from column 2, which knocks it out of the 3 box-3
        // cells that sit in column 2, leaving exactly the other 6 box-3 cells as candidates.
        val board: SudokuGrid = List(9) { r -> List(9) { c -> if (r == 2 && c == 2) 4 else null } }
        val state = createBitmaskStateFromBoard(board)

        val matches = getHouseDigitMatches(state, boxCellIndexes[3], 4)

        assertEquals(
            listOf(
                getCellIndex(3, 0), getCellIndex(3, 1),
                getCellIndex(4, 0), getCellIndex(4, 1),
                getCellIndex(5, 0), getCellIndex(5, 1),
            ),
            matches,
        )
    }

    // --- isSameCell / arePeerCells -----------------------------------------------------------

    @Test
    fun `isSameCell is true only for matching row and col`() {
        assertTrue(isSameCell(SudokuCellRef(2, 3), SudokuCellRef(2, 3)))
        assertFalse(isSameCell(SudokuCellRef(2, 3), SudokuCellRef(2, 4)))
    }

    @Test
    fun `arePeerCells is true for cells sharing a house and false otherwise`() {
        assertTrue(arePeerCells(SudokuCellRef(0, 0), SudokuCellRef(0, 5))) // same row
        assertFalse(arePeerCells(SudokuCellRef(0, 0), SudokuCellRef(5, 5))) // different row/col/box
    }
}
