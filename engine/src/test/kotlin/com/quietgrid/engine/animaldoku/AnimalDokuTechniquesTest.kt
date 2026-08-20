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
}
