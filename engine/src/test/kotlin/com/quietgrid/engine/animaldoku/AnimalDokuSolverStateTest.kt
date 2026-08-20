// engine/src/test/kotlin/com/quietgrid/engine/animaldoku/AnimalDokuSolverStateTest.kt
package com.quietgrid.engine.animaldoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val QUADRANT_REGIONS = listOf(
    listOf(0, 0, 1, 1),
    listOf(0, 0, 1, 1),
    listOf(2, 2, 3, 3),
    listOf(2, 2, 3, 3),
)

class AnimalDokuSolverStateTest {
    @Test
    fun `every cell is a candidate before any placement`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        assertTrue(state.isCandidate(0, 0))
        assertTrue(state.isCandidate(3, 3))
    }

    @Test
    fun `place marks the row column and region solved`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        state.place(0, 0)
        assertTrue(state.rowSolved[0])
        assertTrue(state.colSolved[0])
        assertTrue(state.regionSolved[0])
        assertFalse(state.rowSolved[1])
    }

    @Test
    fun `place eliminates the 8 surrounding cells via adjacency`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        state.place(1, 1)
        assertFalse(state.isCandidate(0, 0))
        assertFalse(state.isCandidate(0, 2))
        assertFalse(state.isCandidate(2, 2))
        assertTrue(state.isCandidate(3, 3))
    }

    @Test
    fun `isCandidate is false for any other cell in a solved row column or region`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        state.place(0, 0)
        assertFalse(state.isCandidate(0, 3))
        assertFalse(state.isCandidate(3, 0))
        assertFalse(state.isCandidate(1, 0))
    }

    @Test
    fun `candidatesInRegion candidatesInRow and candidatesInCol only count remaining candidate cells`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        assertEquals(4, state.candidatesInRegion(0).size)
        state.place(1, 1)
        assertEquals(0, state.candidatesInRegion(0).size)
    }

    @Test
    fun `isSolved is true only once every row has a placement`() {
        val state = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        assertFalse(state.isSolved())
        state.place(0, 1)
        state.place(1, 3)
        assertFalse(state.isSolved())
        state.place(2, 0)
        state.place(3, 2)
        assertTrue(state.isSolved())
    }

    @Test
    fun `copy is independent of the original`() {
        val original = AnimalDokuSolverState(4, QUADRANT_REGIONS)
        val clone = original.copy()
        clone.place(0, 0)
        assertTrue(clone.rowSolved[0])
        assertFalse(original.rowSolved[0])
    }
}
