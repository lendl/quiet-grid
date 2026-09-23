package com.quietgrid.engine.battleship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleshipSolverStateTest {
    @Test
    fun `hasContradiction is false on a fresh state`() {
        val state = BattleshipSolverState(3, intArrayOf(1, 1, 1), intArrayOf(1, 1, 1), listOf(1, 1, 1))
        assertFalse(state.hasContradiction())
    }

    @Test
    fun `hasContradiction is true when a row clue is exceeded`() {
        val state = BattleshipSolverState(2, intArrayOf(0, 0), intArrayOf(1, 1), listOf(1, 1))
        state.grid[0][0] = BattleshipCell.SHIP
        assertTrue(state.hasContradiction())
    }

    @Test
    fun `hasContradiction is true when a row cannot reach its clue`() {
        val state = BattleshipSolverState(2, intArrayOf(2, 0), intArrayOf(1, 1), listOf(1, 1))
        state.grid[0][0] = BattleshipCell.WATER
        state.grid[0][1] = BattleshipCell.WATER
        assertTrue(state.hasContradiction())
    }

    @Test
    fun `copy is an independent deep copy of the grid`() {
        val state = BattleshipSolverState(2, intArrayOf(1, 0), intArrayOf(1, 0), listOf(1))
        val clone = state.copy()
        clone.grid[0][0] = BattleshipCell.SHIP
        assertEquals(BattleshipCell.UNKNOWN, state.grid[0][0])
    }

    @Test
    fun `isSolved is true only when no cell is unknown`() {
        val state = BattleshipSolverState(1, intArrayOf(1), intArrayOf(1), listOf(1))
        assertFalse(state.isSolved())
        state.grid[0][0] = BattleshipCell.SHIP
        assertTrue(state.isSolved())
    }
}
