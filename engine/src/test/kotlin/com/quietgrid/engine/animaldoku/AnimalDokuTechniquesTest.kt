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

private val CHAIN_REGIONS = listOf(
    listOf(0, 0, 2, 3, 3, 3, 3, 3),
    listOf(0, 0, 2, 3, 3, 3, 1, 1),
    listOf(2, 2, 2, 3, 3, 3, 3, 1),
    listOf(2, 2, 3, 3, 2, 3, 3, 1),
    listOf(5, 2, 2, 2, 2, 5, 3, 4),
    listOf(5, 5, 5, 5, 5, 5, 4, 4),
    listOf(6, 5, 5, 5, 7, 5, 5, 4),
    listOf(6, 6, 5, 5, 7, 7, 7, 4),
)

private val CHAIN_SOLUTION = listOf(1, 6, 2, 5, 7, 3, 0, 4)

private val CHAIN_MARKS = listOf(
    0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6, 0 to 7, 1 to 0, 1 to 1, 1 to 7, 2 to 0, 2 to 1, 2 to 6, 2 to 7,
    3 to 0, 3 to 1, 3 to 6, 3 to 7, 4 to 0, 4 to 1, 4 to 2, 4 to 3, 4 to 4, 4 to 6, 5 to 0, 5 to 1,
    6 to 1, 6 to 2, 6 to 3, 6 to 5, 6 to 6, 6 to 7, 7 to 2, 7 to 3, 7 to 6, 7 to 7,
)

private fun chainState(): AnimalDokuSolverState {
    val state = AnimalDokuSolverState(8, CHAIN_REGIONS)
    state.place(1, 6)
    CHAIN_MARKS.forEach { (row, col) -> state.eliminated[row][col] = true }
    return state
}

private fun assertChainReplays(state: AnimalDokuSolverState, step: AnimalDokuStep.Elimination) {
    val (row, col) = step.cells.single()
    val hypothesis = state.copy()
    hypothesis.place(row, col)
    step.forced.forEach { link ->
        assertEquals(listOf(link.row to link.col), hypothesis.candidatesIn(link.unit))
        hypothesis.place(link.row, link.col)
    }
    val emptyUnit = step.sources.single()
    assertTrue(emptyUnit.cells(state.regionOf).none { (r, c) -> hypothesis.placedCol[r] == c })
    assertTrue(hypothesis.candidatesIn(emptyUnit).isEmpty())
    assertEquals(1 + step.forced.size, step.chainDepth)
}

class AnimalDokuTechniquesTest {
    @Test
    fun `findSingleton returns null when every region row and column has multiple candidates`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        assertNull(findSingleton(state))
    }

    @Test
    fun `findSingleton finds a region narrowed to exactly one remaining cell`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        state.eliminated[0][2] = true
        state.eliminated[0][3] = true
        state.eliminated[1][3] = true
        val move = findSingleton(state)
        assertEquals(AnimalDokuStep.Placement(1, 2, AnimalDokuTechnique.SINGLETON, AnimalDokuUnit(AnimalDokuUnitKind.REGION, 1)), move)
    }

    @Test
    fun `findSingleton finds a row narrowed to exactly one remaining cell`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        state.eliminated[2][0] = true
        state.eliminated[2][1] = true
        state.eliminated[2][3] = true
        val move = findSingleton(state)
        assertEquals(AnimalDokuStep.Placement(2, 2, AnimalDokuTechnique.SINGLETON, AnimalDokuUnit(AnimalDokuUnitKind.ROW, 2)), move)
    }

    @Test
    fun `findConfinement eliminates other regions from a line a region is confined to`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        state.eliminated[1][0] = true
        state.eliminated[1][1] = true
        val move = findConfinement(state)
        checkNotNull(move)
        assertEquals(AnimalDokuTechnique.CONFINEMENT, move.technique)
        assertTrue((0 to 2) in move.cells)
        assertTrue((0 to 3) in move.cells)
    }

    @Test
    fun `findConfinement eliminates a region from cells outside a line it is forced into`() {
        val columnRegions = List(4) { row -> List(4) { col -> col } }
        val state = AnimalDokuSolverState(4, columnRegions)
        state.eliminated[0][1] = true
        state.eliminated[0][2] = true
        state.eliminated[0][3] = true
        val move = findConfinement(state)
        checkNotNull(move)
        assertEquals(AnimalDokuTechnique.CONFINEMENT, move.technique)
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
        val stripRegions = List(6) { row -> List(6) { col -> col } }
        val state = AnimalDokuSolverState(6, stripRegions)
        for (row in listOf(0, 1, 3, 5)) {
            state.eliminated[row][0] = true
            state.eliminated[row][1] = true
        }
        val move = findPairing(state, 2)
        checkNotNull(move)
        assertEquals(AnimalDokuTechnique.PAIRING_2, move.technique)
        assertTrue((2 to 2) in move.cells)
        assertTrue((4 to 3) in move.cells)
    }

    @Test
    fun `findPairing K equals 2 confines two regions to two rows that hold only those regions`() {
        val regions = listOf(
            listOf(0, 0, 0, 1, 1, 1),
            listOf(0, 0, 0, 1, 1, 1),
            listOf(0, 2, 3, 4, 5, 1),
            listOf(2, 2, 3, 4, 5, 5),
            listOf(2, 3, 3, 4, 4, 5),
            listOf(2, 3, 4, 4, 5, 5),
        )
        val state = AnimalDokuSolverState(6, regions)
        val move = findPairing(state, 2)
        checkNotNull(move)
        assertEquals(AnimalDokuTechnique.PAIRING_2, move.technique)
        assertEquals(setOf(2 to 0, 2 to 5), move.cells.toSet())
    }

    @Test
    fun `findPairing returns null when fewer than K regions are confined to K lines`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        assertNull(findPairing(state, 2))
    }

    @Test
    fun `findChainContradiction finds depth 1 when a hypothesis directly empties another region`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        state.eliminated[0][3] = true
        state.eliminated[1][2] = true
        state.eliminated[1][3] = true
        val move = findChainContradiction(state)
        checkNotNull(move)
        assertEquals(AnimalDokuTechnique.CHAIN, move.technique)
        assertEquals(1, move.chainDepth)
        assertEquals(listOf(0 to 0), move.cells)
    }

    @Test
    fun `findChainContradiction returns null when no hypothesis leads to a contradiction`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        state.place(0, 1)
        state.place(1, 3)
        state.place(2, 0)
        assertNull(findChainContradiction(state))
    }

    @Test
    fun `findChainContradiction keeps the first chain it finds for grading`() {
        val move = checkNotNull(findChainContradiction(chainState()))
        assertEquals(listOf(0 to 0), move.cells)
        assertEquals(6, move.chainDepth)
    }

    @Test
    fun `findShortestChainContradiction picks a shorter chain than the first one found`() {
        val state = chainState()
        val move = checkNotNull(findShortestChainContradiction(state))
        assertEquals(AnimalDokuTechnique.CHAIN, move.technique)
        assertTrue(move.chainDepth <= 3)
        move.cells.forEach { (row, col) -> assertTrue(CHAIN_SOLUTION[row] != col) }
        assertChainReplays(state, move)
    }

    @Test
    fun `findShortestChainContradiction lists no forced animals for a direct contradiction`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        state.eliminated[0][3] = true
        state.eliminated[1][2] = true
        state.eliminated[1][3] = true
        val move = checkNotNull(findShortestChainContradiction(state))
        assertEquals(1, move.chainDepth)
        assertTrue(move.forced.isEmpty())
        assertChainReplays(state, move)
    }

    @Test
    fun `findShortestChainContradiction returns null when no hypothesis leads to a contradiction`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        state.place(0, 1)
        state.place(1, 3)
        state.place(2, 0)
        assertNull(findShortestChainContradiction(state))
    }
}
