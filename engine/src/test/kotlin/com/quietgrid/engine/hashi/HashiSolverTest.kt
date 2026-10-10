package com.quietgrid.engine.hashi

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HashiSolverTest {
    private val square = HashiBoard(
        3,
        3,
        listOf(
            HashiIsland(0, 0, 1),
            HashiIsland(0, 2, 1),
            HashiIsland(2, 0, 2),
            HashiIsland(2, 2, 2),
        ),
    )

    private val hard = HashiBoard(
        10,
        10,
        listOf(
            HashiIsland(0, 1, 3), HashiIsland(0, 3, 4), HashiIsland(0, 7, 2), HashiIsland(0, 9, 2),
            HashiIsland(1, 8, 1), HashiIsland(2, 1, 2), HashiIsland(2, 3, 3), HashiIsland(2, 6, 2),
            HashiIsland(2, 9, 3), HashiIsland(3, 7, 1), HashiIsland(4, 4, 1), HashiIsland(4, 6, 4),
            HashiIsland(4, 8, 4), HashiIsland(5, 3, 2), HashiIsland(5, 5, 1), HashiIsland(6, 1, 4),
            HashiIsland(6, 4, 2), HashiIsland(6, 8, 2), HashiIsland(7, 2, 1), HashiIsland(7, 6, 5),
            HashiIsland(7, 9, 3), HashiIsland(9, 1, 2), HashiIsland(9, 6, 3), HashiIsland(9, 8, 1),
        ),
    )

    @Test
    fun `counting alone solves a chain of islands`() {
        val board = HashiBoard(3, 3, listOf(HashiIsland(0, 0, 2), HashiIsland(0, 2, 1), HashiIsland(2, 0, 1)))
        val result = solveHashi(board)
        assertTrue(result.solved)
        assertTrue(result.steps.all { it.technique == HashiTechnique.CAPACITY || it.technique == HashiTechnique.CROSSING })
        assertEquals(Difficulty.EASY, classifyHashiDifficulty(result))
    }

    @Test
    fun `two ones never join each other when that would cut them off`() {
        val result = solveHashi(square)
        assertTrue(result.solved)
        assertTrue(result.steps.any { it.technique == HashiTechnique.ISOLATION })
        assertTrue(result.steps.none { it.technique == HashiTechnique.CONTRADICTION })
        val counts = result.counts!!
        assertEquals(0, counts[square.edgeIndex(0, 1)])
        assertEquals(1, counts[square.edgeIndex(2, 3)])
    }

    @Test
    fun `a hard puzzle needs a contradiction chain and still solves uniquely`() {
        val result = solveHashi(hard)
        assertTrue(result.solved)
        assertEquals(Difficulty.HARD, classifyHashiDifficulty(result))
        assertEquals(1, countHashiSolutions(hard))
        assertTrue(isHashiSolved(hard, result.counts!!))
    }

    @Test
    fun `without contradiction chains the hard puzzle stalls`() {
        assertFalse(solveHashi(hard, maxChainDepth = 0).solved)
    }

    @Test
    fun `a puzzle with two answers is not solved by logic`() {
        val ambiguous = HashiBoard(
            3,
            3,
            listOf(HashiIsland(0, 0, 3), HashiIsland(0, 2, 3), HashiIsland(2, 0, 3), HashiIsland(2, 2, 3)),
        )
        assertEquals(2, countHashiSolutions(ambiguous))
        assertFalse(solveHashi(ambiguous).solved)
    }
}
