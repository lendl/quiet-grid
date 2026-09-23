package com.quietgrid.engine.battleship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleshipTechniquesTest {
    @Test
    fun `findStructuralWater marks unknown neighbors of a ship cell`() {
        val state = BattleshipSolverState(3, intArrayOf(1, 0, 0), intArrayOf(1, 0, 0), listOf(1))
        state.grid[0][0] = BattleshipCell.SHIP
        val step = findStructuralWater(state) as BattleshipStep.Water
        assertEquals(BattleshipTechnique.STRUCTURAL_WATER, step.technique)
        assertTrue((0 to 1) in step.cells)
        assertTrue((1 to 0) in step.cells)
        assertTrue((1 to 1) in step.cells)
    }

    @Test
    fun `findStructuralWater does not water a lone ship cell's in-line neighbors when a longer ship remains unplaced`() {
        val state = BattleshipSolverState(3, intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), listOf(2))
        state.grid[1][1] = BattleshipCell.SHIP
        val step = findStructuralWater(state) as BattleshipStep.Water
        assertEquals(setOf(0 to 0, 0 to 2, 2 to 0, 2 to 2), step.cells.toSet())
        assertTrue((0 to 1) !in step.cells)
        assertTrue((2 to 1) !in step.cells)
        assertTrue((1 to 0) !in step.cells)
        assertTrue((1 to 2) !in step.cells)
    }

    @Test
    fun `findLineSaturation forces remaining unknowns to water once the row clue is met`() {
        val state = BattleshipSolverState(3, intArrayOf(1, 0, 0), intArrayOf(1, 0, 0), listOf(1))
        state.grid[0][0] = BattleshipCell.SHIP
        state.grid[0][1] = BattleshipCell.WATER
        val step = findLineSaturation(state) as BattleshipStep.Water
        assertEquals(listOf(0 to 2), step.cells)
    }

    @Test
    fun `findLineExhaustion forces remaining unknowns to ship when the row needs all of them`() {
        val state = BattleshipSolverState(3, intArrayOf(2, 0, 0), intArrayOf(1, 1, 0), listOf(2))
        state.grid[0][2] = BattleshipCell.WATER
        val step = findLineExhaustion(state) as BattleshipStep.Ship
        assertEquals(setOf(0 to 0, 0 to 1), step.cells.toSet())
    }

    @Test
    fun `findFleetElimination forces water when a bounded run is shorter than any remaining ship`() {
        val openState = BattleshipSolverState(3, intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), listOf(1))
        assertNull(findFleetElimination(openState))

        val gappedState = BattleshipSolverState(3, intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), listOf(2))
        gappedState.grid[0][0] = BattleshipCell.WATER
        gappedState.grid[0][2] = BattleshipCell.WATER
        val step = findFleetElimination(gappedState) as BattleshipStep.Water
        assertEquals(listOf(0 to 1), step.cells)
    }

    @Test
    fun `findFleetLengthMatch forces the sole candidate run for the last remaining ship`() {
        val state = BattleshipSolverState(3, intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), listOf(2))
        state.grid[0][0] = BattleshipCell.WATER
        state.grid[1][0] = BattleshipCell.WATER
        state.grid[1][1] = BattleshipCell.WATER
        state.grid[1][2] = BattleshipCell.WATER
        state.grid[2][0] = BattleshipCell.WATER
        state.grid[2][1] = BattleshipCell.WATER
        state.grid[2][2] = BattleshipCell.WATER
        val step = findFleetLengthMatch(state) as BattleshipStep.Ship
        assertEquals(setOf(0 to 1, 0 to 2), step.cells.toSet())
    }

    @Test
    fun `findProbing resolves a cell whose ship hypothesis contradicts the clues`() {
        val state = BattleshipSolverState(2, intArrayOf(0, 1), intArrayOf(1, 0), listOf(1))
        val step = findProbing(state) as BattleshipStep.Water
        assertEquals(0 to 0, step.cells.single())
    }
}
