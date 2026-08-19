// engine/src/test/kotlin/com/quietgrid/engine/animaldoku/AnimalDokuTechniquesTest.kt
package com.quietgrid.engine.animaldoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val QUADRANT_REGIONS = listOf(
    listOf(0, 0, 1, 1),
    listOf(0, 0, 1, 1),
    listOf(2, 2, 3, 3),
    listOf(2, 2, 3, 3),
)

class AnimalDokuTechniquesTest {
    @Test
    fun `findSingleton returns null when every region row and column has multiple candidates`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        assertNull(findSingleton(state))
    }

    @Test
    fun `findSingleton finds a region narrowed to exactly one remaining cell`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        // Placing at (1,1) eliminates (0,0) via adjacency, and region 0's only other cells are (0,1)/(1,0)
        // which are also king-adjacent to (1,1) -> region 0 has zero left, not a useful singleton case.
        // Use region 3 instead: place at (0,0) leaves region 3's four cells untouched (far away),
        // so seed a targeted scenario by eliminating three of region 1's four cells directly.
        state.eliminated[0][2] = true
        state.eliminated[0][3] = true
        state.eliminated[1][3] = true
        val move = findSingleton(state)
        assertEquals(AnimalDokuStep.Placement(1, 2, AnimalDokuTechnique.SINGLETON), move)
    }

    @Test
    fun `findSingleton finds a row narrowed to exactly one remaining cell`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        state.eliminated[2][0] = true
        state.eliminated[2][1] = true
        state.eliminated[2][3] = true
        val move = findSingleton(state)
        assertEquals(AnimalDokuStep.Placement(2, 2, AnimalDokuTechnique.SINGLETON), move)
    }

    @Test
    fun `findConfinement eliminates other regions from a line a region is confined to`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        // Confine region 0's remaining candidates to row 0 by eliminating its row-1 cells.
        state.eliminated[1][0] = true
        state.eliminated[1][1] = true
        val move = findConfinement(state)
        checkNotNull(move)
        assertEquals(AnimalDokuTechnique.CONFINEMENT, move.technique)
        // Row 0's cells outside region 0 (i.e. (0,2) and (0,3), region 1) must be eliminated.
        assertTrue((0 to 2) in move.cells)
        assertTrue((0 to 3) in move.cells)
    }

    @Test
    fun `findConfinement eliminates a region from cells outside a line it is forced into`() {
        // Column-strip regions (region i = column i) so a region's own column-confinement never
        // produces a non-empty elimination (nothing else shares that column), isolating this test
        // to the line-confined-to-region direction specifically.
        val columnRegions = List(4) { row -> List(4) { col -> col } }
        val state = AnimalDokuSolverState(4, columnRegions)
        // Row 0's only remaining candidate is region 0's cell (0,0): eliminate row 0's other columns.
        state.eliminated[0][1] = true
        state.eliminated[0][2] = true
        state.eliminated[0][3] = true
        val move = findConfinement(state)
        checkNotNull(move)
        assertEquals(AnimalDokuTechnique.CONFINEMENT, move.technique)
        // Region 0's cells outside row 0 (i.e. (1,0), (2,0), (3,0)) must be eliminated.
        assertTrue((1 to 0) in move.cells)
        assertTrue((2 to 0) in move.cells)
        assertTrue((3 to 0) in move.cells)
    }

    @Test
    fun `findConfinement returns null when nothing is confined`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        assertNull(findConfinement(state))
    }

    @Test
    fun `findPairing K equals 2 eliminates other regions from two lines jointly claimed by two regions`() {
        // 6x6 grid, 6 single-column vertical strip regions (region i = column i) so region 0 and
        // region 1's candidates can be confined to the same two rows by elimination.
        val stripRegions = List(6) { row -> List(6) { col -> col } }
        val state = AnimalDokuSolverState(6, stripRegions)
        // Confine region 0 (column 0) and region 1 (column 1) to rows 2 and 4 only.
        for (row in listOf(0, 1, 3, 5)) {
            state.eliminated[row][0] = true
            state.eliminated[row][1] = true
        }
        val move = findPairing(state, 2)
        checkNotNull(move)
        assertEquals(AnimalDokuTechnique.PAIRING_2, move.technique)
        // Every other region's candidates in row 2 and row 4 must be eliminated.
        assertTrue((2 to 2) in move.cells)
        assertTrue((4 to 3) in move.cells)
    }

    @Test
    fun `findPairing returns null when fewer than K regions are confined to K lines`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        assertNull(findPairing(state, 2))
    }

    @Test
    fun `findChainContradiction finds depth 1 when a hypothesis directly empties another region`() {
        // 4x4, regions 0 and 1 both confined to row 0's two halves; region 2/3 span rows 2-3.
        // Region 1 has only one remaining candidate, (0,2): eliminate its other three cells.
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        state.eliminated[0][3] = true
        state.eliminated[1][2] = true
        state.eliminated[1][3] = true
        // Region 0 is left with all 4 candidates open: (0,0), (0,1), (1,0), (1,1), tried in that
        // (row-major) order by findChainContradiction. The first hypothesis tried, (0,0), places
        // an animal in row 0 -- which solves row 0 outright, not just adjacency-eliminates
        // neighbors. Region 1's sole remaining candidate (0,2) is also in row 0, so it is
        // disqualified by the row being solved (regardless of adjacency) the moment (0,0) is
        // hypothesized. That leaves region 1 with zero candidates and it is not yet solved ->
        // an immediate contradiction, with no intermediate forced-singleton placements needed ->
        // chainDepth 1. Since (0,0) is the first candidate iterated, it is what gets eliminated.
        val move = findChainContradiction(state)
        checkNotNull(move)
        assertEquals(AnimalDokuTechnique.CHAIN, move.technique)
        assertEquals(1, move.chainDepth)
        assertEquals(listOf(0 to 0), move.cells)
    }

    @Test
    fun `findChainContradiction returns null when no hypothesis leads to a contradiction`() {
        // A fully untouched 4x4 board is NOT a safe "no contradiction" fixture here: it turns out
        // to already contain a genuine depth-3 chain (hypothesizing region 0 at (1,1) forces row 0
        // to col 3, which forces rows 2/3 into columns {0,2}, both of which are king-adjacent to
        // row 1's col 1 -- no valid completion exists). So instead, build a state from an actual
        // known-valid partial solution (row0->col1, row1->col3, row2->col0, part of the full valid
        // solution row0=1,row1=3,row2=0,row3=2) via real placements, leaving region 3 with its one
        // remaining candidate, (3,2) -- which the known solution also uses. Every hypothesis tried
        // is therefore guaranteed satisfiable, so no contradiction can be found.
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        state.place(0, 1)
        state.place(1, 3)
        state.place(2, 0)
        assertNull(findChainContradiction(state))
    }
}
