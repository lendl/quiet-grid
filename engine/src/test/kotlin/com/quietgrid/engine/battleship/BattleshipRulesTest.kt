package com.quietgrid.engine.battleship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun gridOf(vararg rows: String): List<List<BattleshipCell>> = rows.map { row ->
    row.map { c ->
        when (c) {
            'S' -> BattleshipCell.SHIP
            'W' -> BattleshipCell.WATER
            else -> BattleshipCell.UNKNOWN
        }
    }
}

class BattleshipRulesTest {
    @Test
    fun `confirmedShipLengths only counts runs bounded by water or edge`() {
        val grid = gridOf(
            "SWU",
            "WWW",
            "UWS",
        )
        assertEquals(listOf(1, 1), confirmedShipLengths(grid).sorted())
    }

    @Test
    fun `confirmedShipLengths does not confirm an isolated cell with an unknown neighbor`() {
        val grid = gridOf(
            "SWU",
            "WWU",
            "UUS",
        )
        assertEquals(listOf(1), confirmedShipLengths(grid))
    }

    @Test
    fun `confirmedShipLengths ignores a ship run still open at one end`() {
        val grid = gridOf(
            "SSU",
            "WWU",
            "UUU",
        )
        assertTrue(confirmedShipLengths(grid).isEmpty())
    }

    @Test
    fun `remainingFleetCounts subtracts confirmed ships from the fleet`() {
        val grid = gridOf(
            "SWU",
            "WWU",
        )
        val remaining = remainingFleetCounts(grid, fleet = listOf(1, 2))
        assertEquals(0, remaining.getValue(1))
        assertEquals(1, remaining.getValue(2))
    }

    @Test
    fun `isBattleshipSolved requires every cell to match the solution exactly`() {
        val solutionCells = setOf(0 to 0)
        val solved = gridOf("SW", "WW")
        val unsolved = gridOf("SU", "WW")
        assertTrue(isBattleshipSolved(solved, solutionCells))
        assertTrue(!isBattleshipSolved(unsolved, solutionCells))
    }
}
