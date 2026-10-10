package com.quietgrid.engine.hashi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HashiBoardTest {
    private val plus = HashiBoard(
        5,
        5,
        listOf(
            HashiIsland(0, 2, 1),
            HashiIsland(2, 0, 1),
            HashiIsland(2, 4, 1),
            HashiIsland(4, 2, 1),
        ),
    )

    @Test
    fun `each island links to its nearest neighbour in every direction`() {
        assertEquals(2, plus.edges.size)
        assertTrue(plus.edgeIndex(0, 3) >= 0)
        assertTrue(plus.edgeIndex(1, 2) >= 0)
        assertEquals(-1, plus.edgeIndex(0, 1))
    }

    @Test
    fun `a horizontal and a vertical bridge through the same gap cross`() {
        val vertical = plus.edgeIndex(0, 3)
        val horizontal = plus.edgeIndex(1, 2)
        assertTrue(horizontal in plus.crossings[vertical])
        assertTrue(vertical in plus.crossings[horizontal])
    }

    @Test
    fun `islands block the line of sight`() {
        val board = HashiBoard(1, 5, listOf(HashiIsland(0, 0, 1), HashiIsland(0, 2, 2), HashiIsland(0, 4, 1)))
        assertEquals(-1, board.edgeIndex(0, 2))
        assertTrue(board.edgeIndex(0, 1) >= 0)
        assertTrue(board.edgeIndex(1, 2) >= 0)
    }

    @Test
    fun `a solution must match every count, stay connected and never cross`() {
        val board = HashiBoard(1, 5, listOf(HashiIsland(0, 0, 1), HashiIsland(0, 2, 2), HashiIsland(0, 4, 1)))
        assertTrue(isHashiSolved(board, intArrayOf(1, 1)))
        assertFalse(isHashiSolved(board, intArrayOf(1, 0)))
        assertFalse(isHashiSolved(plus, intArrayOf(1, 1)))
    }

    @Test
    fun `bridges round trip through edge counts`() {
        val board = HashiBoard(1, 5, listOf(HashiIsland(0, 0, 2), HashiIsland(0, 2, 3), HashiIsland(0, 4, 1)))
        val bridges = listOf(HashiBridge(0, 1, 2), HashiBridge(1, 2, 1))
        assertEquals(bridges, board.bridgesFromCounts(board.countsFromBridges(bridges)))
    }
}
