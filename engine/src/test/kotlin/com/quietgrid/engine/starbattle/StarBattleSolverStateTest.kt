package com.quietgrid.engine.starbattle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarBattleSolverStateTest {
    private val regions = listOf(
        listOf(0, 0, 1, 1),
        listOf(0, 0, 1, 1),
        listOf(2, 2, 3, 3),
        listOf(2, 2, 3, 3),
    )

    @Test
    fun `remaining starts at k for every unit`() {
        val state = StarBattleSolverState(4, 2, regions)
        assertEquals(2, state.rowRemaining(0))
        assertEquals(2, state.colRemaining(0))
        assertEquals(2, state.regionRemaining(0))
    }

    @Test
    fun `place decrements remaining and eliminates the 8 neighbors`() {
        val state = StarBattleSolverState(4, 2, regions)
        state.place(0, 0)
        assertEquals(1, state.rowRemaining(0))
        assertEquals(1, state.colRemaining(0))
        assertEquals(1, state.regionRemaining(0))
        assertFalse(state.isCandidate(0, 1))
        assertFalse(state.isCandidate(1, 0))
        assertFalse(state.isCandidate(1, 1))
    }

    @Test
    fun `isCandidate is false once a unit reaches k`() {
        val state = StarBattleSolverState(4, 1, regions)
        state.place(0, 0)
        assertFalse(state.isCandidate(0, 3))
        assertFalse(state.isCandidate(3, 0))
    }

    @Test
    fun `isSolved is true only when every row has k stars`() {
        val state = StarBattleSolverState(4, 2, regions)
        assertFalse(state.isSolved())
        state.place(0, 0)
        state.place(0, 3)
        state.place(1, 1)
        state.place(1, 2)
        state.place(2, 0)
        state.place(2, 3)
        state.place(3, 1)
        state.place(3, 2)
        assertTrue(state.isSolved())
    }

    @Test
    fun `copy is independent of the original`() {
        val state = StarBattleSolverState(4, 2, regions)
        val clone = state.copy()
        clone.place(0, 0)
        assertEquals(2, state.rowRemaining(0))
        assertEquals(1, clone.rowRemaining(0))
    }
}
