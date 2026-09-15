package com.quietgrid.engine.starbattle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StarBattleTechniquesTest {
    @Test
    fun `forced placement fires when a region's remaining equals its candidate count`() {
        val regions = listOf(
            listOf(0, 0, 1),
            listOf(0, 1, 1),
            listOf(2, 2, 2),
        )
        val state = StarBattleSolverState(3, 1, regions)
        state.eliminated[0][0] = true
        state.eliminated[0][1] = true
        val move = findForcedPlacement(state)
        assertTrue(move != null && move.technique == StarBattleTechnique.FORCED_PLACEMENT)
        assertEquals(listOf(1 to 0), move!!.cells)
    }

    @Test
    fun `forced placement can place two cells at once when remaining is 2`() {
        val regions = List(4) { r -> List(4) { c -> if (r < 2) 0 else if (c < 2) 1 else 2 }.let { line -> if (r >= 2) line.toMutableList().also { it[2] = 3; it[3] = 3 } else line } }
        val state = StarBattleSolverState(4, 2, regions)
        for (row in 1 until 4) for (col in 0 until 4) state.eliminated[row][col] = true
        state.eliminated[0][1] = true
        state.eliminated[0][3] = true
        val move = findForcedPlacement(state)
        assertTrue(move != null)
        assertEquals(listOf(0 to 0, 0 to 2), move!!.cells)
        assertEquals(StarBattleTechnique.FORCED_PLACEMENT, move.technique)
    }

    @Test
    fun `confinement does not fire when the confining region's remaining is less than the row's remaining`() {
        val regions = listOf(
            listOf(1, 1, 2, 2),
            listOf(0, 0, 2, 2),
            listOf(3, 3, 1, 1),
            listOf(3, 3, 1, 1),
        )
        val state = StarBattleSolverState(4, 2, regions)
        state.regionCount[0] = 1
        assertEquals(1, state.regionRemaining(0))
        assertEquals(2, state.rowRemaining(1))
        assertNull(findConfinement(state))
    }

    @Test
    fun `confinement fires when the confining region's remaining matches the row's remaining exactly`() {
        val regions = listOf(
            listOf(1, 1, 2, 2),
            listOf(0, 0, 2, 2),
            listOf(3, 3, 1, 1),
            listOf(3, 3, 1, 1),
        )
        val state = StarBattleSolverState(4, 2, regions)
        assertEquals(2, state.regionRemaining(0))
        assertEquals(2, state.rowRemaining(1))
        val elimination = findConfinement(state)
        assertTrue(elimination != null)
        assertTrue(elimination!!.cells.contains(1 to 2) && elimination.cells.contains(1 to 3))
    }

    @Test
    fun `confinement labels a region-confined-to-row elimination as structural when the region's raw cells lie entirely in that row`() {
        val regions = listOf(
            listOf(0, 0, 1, 1),
            listOf(1, 1, 2, 2),
            listOf(3, 3, 2, 2),
            listOf(3, 3, 3, 3),
        )
        val state = StarBattleSolverState(4, 1, regions)
        val elimination = findConfinement(state)
        assertTrue(elimination != null)
        assertEquals(StarBattleTechnique.STRUCTURAL_CONFINEMENT, elimination!!.technique)
        assertEquals(listOf(0 to 2, 0 to 3), elimination.cells)
    }

    @Test
    fun `confinement labels a region-confined-to-row elimination as non-structural when confinement only holds after eliminations`() {
        val regions = listOf(
            listOf(0, 1, 1, 2),
            listOf(0, 1, 2, 2),
            listOf(3, 3, 2, 2),
            listOf(3, 3, 3, 3),
        )
        val state = StarBattleSolverState(4, 1, regions)
        state.eliminated[1][0] = true
        val elimination = findConfinement(state)
        assertTrue(elimination != null)
        assertEquals(StarBattleTechnique.CONFINEMENT, elimination!!.technique)
        assertEquals(listOf(0 to 1, 0 to 2, 0 to 3), elimination.cells)
    }

    @Test
    fun `confinement fires via the row-confined-to-region path eliminating the region's other cells`() {
        val regions = listOf(
            listOf(0, 0, 0, 0),
            listOf(0, 1, 2, 3),
            listOf(1, 2, 3, 1),
            listOf(2, 3, 1, 2),
        )
        val state = StarBattleSolverState(4, 1, regions)
        val elimination = findConfinement(state)
        assertTrue(elimination != null)
        assertEquals(listOf(1 to 0), elimination!!.cells)
        assertEquals(StarBattleTechnique.STRUCTURAL_CONFINEMENT, elimination.technique)
    }

    @Test
    fun `confinement fires via the column-confined-to-region path eliminating the region's other cells`() {
        val regions = listOf(
            listOf(0, 0, 1, 2),
            listOf(0, 1, 2, 3),
            listOf(0, 2, 3, 1),
            listOf(0, 3, 1, 2),
        )
        val state = StarBattleSolverState(4, 1, regions)
        val elimination = findConfinement(state)
        assertTrue(elimination != null)
        assertEquals(listOf(0 to 1), elimination!!.cells)
        assertEquals(StarBattleTechnique.STRUCTURAL_CONFINEMENT, elimination.technique)
    }

    @Test
    fun `forced placement fires via the row loop when no region already qualifies`() {
        val regions = listOf(
            listOf(0, 1, 2, 3),
            listOf(0, 1, 2, 3),
            listOf(0, 1, 2, 3),
            listOf(0, 1, 2, 3),
        )
        val state = StarBattleSolverState(4, 1, regions)
        state.eliminated[0][1] = true
        state.eliminated[0][2] = true
        state.eliminated[0][3] = true
        val move = findForcedPlacement(state)
        assertTrue(move != null)
        assertEquals(listOf(0 to 0), move!!.cells)
        assertEquals(StarBattleTechnique.FORCED_PLACEMENT, move.technique)
    }

    @Test
    fun `forced placement fires via the column loop when no region or row already qualifies`() {
        val regions = listOf(
            listOf(0, 0, 0, 0),
            listOf(1, 1, 1, 1),
            listOf(2, 2, 2, 2),
            listOf(3, 3, 3, 3),
        )
        val state = StarBattleSolverState(4, 1, regions)
        state.eliminated[1][0] = true
        state.eliminated[2][0] = true
        state.eliminated[3][0] = true
        val move = findForcedPlacement(state)
        assertTrue(move != null)
        assertEquals(listOf(0 to 0), move!!.cells)
        assertEquals(StarBattleTechnique.FORCED_PLACEMENT, move.technique)
    }
}
