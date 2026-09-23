package com.quietgrid.engine.battleship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleshipSolverTest {
    @Test
    fun `solveBattleship solves a fully-determined 3x3 puzzle`() {
        val result = solveBattleship(
            size = 3,
            rowClues = listOf(0, 1, 0),
            colClues = listOf(0, 1, 0),
            fleet = listOf(1),
        )
        assertTrue(result.solved)
        assertTrue(result.steps.isNotEmpty())
    }

    @Test
    fun `solveBattleship uses a given cell to break an otherwise-ambiguous puzzle`() {
        val ambiguous = solveBattleship(
            size = 3,
            rowClues = listOf(1, 0, 1),
            colClues = listOf(1, 0, 1),
            fleet = listOf(1, 1),
        )
        assertEquals(false, ambiguous.solved)

        val seeded = solveBattleship(
            size = 3,
            rowClues = listOf(1, 0, 1),
            colClues = listOf(1, 0, 1),
            fleet = listOf(1, 1),
            givens = listOf(Triple(0, 0, BattleshipCell.SHIP)),
        )
        assertTrue(seeded.solved)
    }

    @Test
    fun `solveBattleship reports unsolved for a contradictory puzzle`() {
        val result = solveBattleship(
            size = 2,
            rowClues = listOf(1, 1),
            colClues = listOf(1, 1),
            fleet = listOf(1, 1),
        )
        assertEquals(false, result.solved)
    }
}
