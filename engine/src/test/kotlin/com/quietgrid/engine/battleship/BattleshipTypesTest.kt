package com.quietgrid.engine.battleship

import org.junit.Assert.assertEquals
import org.junit.Test

class BattleshipTypesTest {
    @Test
    fun `encode then decode round-trips ship cells`() {
        val ship = listOf(
            listOf(true, false, false),
            listOf(false, false, true),
            listOf(false, true, true),
        )
        val encoded = encodeBattleshipSolution(ship)
        assertEquals("100001011", encoded)
        val decoded = decodeBattleshipSolutionCells(encoded, size = 3)
        assertEquals(setOf(0 to 0, 1 to 2, 2 to 1, 2 to 2), decoded)
    }

    @Test
    fun `encode then decode round-trips givens`() {
        val givens = listOf(
            Triple(0, 0, BattleshipCell.SHIP),
            Triple(1, 1, BattleshipCell.WATER),
        )
        val encoded = encodeBattleshipGivens(givens, size = 2)
        assertEquals("1..0", encoded)
        val decoded = decodeBattleshipGivens(encoded, size = 2)
        assertEquals(givens.toSet(), decoded.toSet())
    }
}
