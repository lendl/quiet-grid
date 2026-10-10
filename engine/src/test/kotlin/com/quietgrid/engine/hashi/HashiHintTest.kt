package com.quietgrid.engine.hashi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HashiHintTest {
    private val islands = listOf(
        HashiIsland(0, 0, 1),
        HashiIsland(0, 2, 1),
        HashiIsland(2, 0, 2),
        HashiIsland(2, 2, 2),
    )
    private val puzzle = HashiPuzzleEntry(
        id = "square",
        rows = 3,
        cols = 3,
        difficulty = "easy",
        islands = islands,
        solution = listOf(HashiBridge(0, 2, 1), HashiBridge(1, 3, 1), HashiBridge(2, 3, 1)),
    )
    private val board = puzzle.toBoard()

    @Test
    fun `the first hint is a bridge the logic forces`() {
        val hint = findHashiHint(puzzle, IntArray(board.edges.size))!!
        assertFalse(hint.isMistake)
        assertEquals(1, hint.count)
        val solution = board.countsFromBridges(puzzle.solution)
        assertTrue(solution[hint.edge] >= hint.count)
    }

    @Test
    fun `a bridge that is not in the solution is flagged as a mistake`() {
        val placed = IntArray(board.edges.size).also { it[board.edgeIndex(0, 1)] = 1 }
        val hint = findHashiHint(puzzle, placed)!!
        assertTrue(hint.isMistake)
        assertEquals(board.edgeIndex(0, 1), hint.edge)
    }

    @Test
    fun `a finished board has no hint`() {
        assertNull(findHashiHint(puzzle, board.countsFromBridges(puzzle.solution)))
    }
}
