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

    @Test
    fun `buildCandidateEliminationMove dedups identical eliminations and sorts by row then col then digit`() {
        val eliminations = listOf(
            getCellIndex(5, 2) to 7,
            getCellIndex(1, 8) to 2,
            getCellIndex(0, 3) to 9,
            getCellIndex(1, 8) to 1,
            getCellIndex(5, 2) to 7,
        )

        val move = buildCandidateEliminationMove(
            SudokuTechnique.NAKED_PAIR,
            eliminations,
            evidenceCells = listOf(getCellIndex(0, 3)),
            houses = emptyList(),
            complexity = 3,
        )

        checkNotNull(move)
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

    @Test
    fun `collectHousesFromIndexes dedups shared houses and sorts by kind then index`() {
        val indexes = listOf(getCellIndex(0, 0), getCellIndex(0, 3), getCellIndex(3, 0))

        val houses = collectHousesFromIndexes(indexes)

        assertEquals(7, houses.size)
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

    @Test
    fun `peerIntersectionIndexes returns exactly the shared peers of both cells`() {
        val result = peerIntersectionIndexes(listOf(getCellIndex(4, 4), getCellIndex(4, 0)))

        val expected = setOf(
            getCellIndex(4, 1), getCellIndex(4, 2), getCellIndex(4, 3),
            getCellIndex(4, 5), getCellIndex(4, 6), getCellIndex(4, 7), getCellIndex(4, 8),
        )
        assertEquals(expected, result.toSet())

        assertFalse(result.contains(getCellIndex(0, 4)))
        assertFalse(result.contains(getCellIndex(3, 1)))
        assertFalse(result.contains(getCellIndex(4, 4)))
        assertFalse(result.contains(getCellIndex(4, 0)))
    }

    @Test
    fun `getHouseDigitMatches returns exactly the house cells that still carry the digit as a candidate`() {
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

    @Test
    fun `isSameCell is true only for matching row and col`() {
        assertTrue(isSameCell(SudokuCellRef(2, 3), SudokuCellRef(2, 3)))
        assertFalse(isSameCell(SudokuCellRef(2, 3), SudokuCellRef(2, 4)))
    }

    @Test
    fun `arePeerCells is true for cells sharing a house and false otherwise`() {
        assertTrue(arePeerCells(SudokuCellRef(0, 0), SudokuCellRef(0, 5)))
        assertFalse(arePeerCells(SudokuCellRef(0, 0), SudokuCellRef(5, 5)))
    }
}
