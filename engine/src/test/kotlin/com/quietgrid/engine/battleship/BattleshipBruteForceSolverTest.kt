package com.quietgrid.engine.battleship

import org.junit.Assert.assertEquals
import org.junit.Test

class BattleshipBruteForceSolverTest {
    @Test
    fun `countBattleshipSolutions returns 1 for a uniquely-determined puzzle`() {
        val count = countBattleshipSolutions(
            size = 3,
            rowClues = listOf(0, 1, 0),
            colClues = listOf(0, 1, 0),
            fleet = listOf(1),
        )
        assertEquals(1, count)
    }

    @Test
    fun `countBattleshipSolutions returns 2 for a genuinely ambiguous puzzle capped at 2`() {
        val count = countBattleshipSolutions(
            size = 3,
            rowClues = listOf(1, 0, 1),
            colClues = listOf(1, 0, 1),
            fleet = listOf(1, 1),
            cap = 2,
        )
        assertEquals(2, count)
    }

    @Test
    fun `countBattleshipSolutions narrows an ambiguous puzzle to 1 once a given cell is fixed`() {
        val withoutGivens = countBattleshipSolutions(
            size = 3,
            rowClues = listOf(1, 0, 1),
            colClues = listOf(1, 0, 1),
            fleet = listOf(1, 1),
            cap = 2,
        )
        assertEquals(2, withoutGivens)

        val withGiven = countBattleshipSolutions(
            size = 3,
            rowClues = listOf(1, 0, 1),
            colClues = listOf(1, 0, 1),
            fleet = listOf(1, 1),
            cap = 2,
            givens = listOf(Triple(0, 0, true)),
        )
        assertEquals(1, withGiven)
    }

    @Test
    fun `countBattleshipSolutions returns 0 when no placement satisfies the clues`() {
        val count = countBattleshipSolutions(
            size = 2,
            rowClues = listOf(2, 2),
            colClues = listOf(2, 2),
            fleet = listOf(1),
        )
        assertEquals(0, count)
    }
}
