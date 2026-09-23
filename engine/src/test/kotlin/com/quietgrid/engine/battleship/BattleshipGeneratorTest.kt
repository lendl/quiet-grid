package com.quietgrid.engine.battleship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleshipGeneratorTest {
    @Test
    fun `generateSolvedBoard places every ship in the fleet with no diagonal touching`() {
        val fleet = listOf(1, 1, 1, 2, 2, 3)
        val board = generateSolvedBoard(6, fleet)
        assertNotNull(board)
        val flatBoard = board!!
        val totalShipCells = flatBoard.sumOf { row -> row.count { it } }
        assertEquals(fleet.sum(), totalShipCells)
        for (row in 0 until 6) {
            for (col in 0 until 6) {
                if (!flatBoard[row][col]) continue
                for ((dr, dc) in listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)) {
                    val nr = row + dr
                    val nc = col + dc
                    if (nr in 0 until 6 && nc in 0 until 6) assertTrue(!flatBoard[nr][nc])
                }
            }
        }
    }

    @Test
    fun `deriveClues counts ship cells per row and column`() {
        val board = listOf(
            listOf(true, false),
            listOf(false, true),
        )
        val (rowClues, colClues) = deriveClues(board)
        assertEquals(listOf(1, 1), rowClues)
        assertEquals(listOf(1, 1), colClues)
    }
}
