package com.quietgrid.engine.flowfree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowFreeSolverTest {
    @Test
    fun `solves a trivial 2x2 single-pair puzzle`() {
        val result = solveFlowFree(size = 2, pairCount = 1, starts = listOf(0 to 0), targets = listOf(1 to 0))
        assertTrue(result.solved)
        assertTrue(isValidFlowFreePaths(2, 1, result.solutionPaths!!))
        assertEquals(0 to 0, flowFreeDecodeCell(result.solutionPaths!![0].first(), 2))
        assertEquals(1 to 0, flowFreeDecodeCell(result.solutionPaths!![0].last(), 2))
    }

    @Test
    fun `solves a 4x4 two-pair puzzle with adjacent same-row endpoints`() {
        val result = solveFlowFree(
            size = 4,
            pairCount = 2,
            starts = listOf(0 to 0, 2 to 0),
            targets = listOf(1 to 0, 3 to 0),
        )
        assertTrue(result.solved)
        assertTrue(isValidFlowFreePaths(4, 2, result.solutionPaths!!))
    }

    @Test
    fun `agrees with a real generated 7x7 puzzle it was built to have a solution for`() {
        val paths = generateFlowFreeSolution(7, pairCount = 6, random = kotlin.random.Random(42))!!
        val endpoints = endpointsOf(7, 6, paths)
        val starts = mutableListOf<Pair<Int, Int>>()
        val targets = mutableListOf<Pair<Int, Int>>()
        for (color in 0 until 6) {
            val cells = (0 until 7).flatMap { r -> (0 until 7).mapNotNull { c -> if (endpoints[r][c] == color) r to c else null } }
            starts += cells[0]
            targets += cells[1]
        }
        val result = solveFlowFree(7, 6, starts, targets)
        assertTrue(result.solved)
        assertTrue(isValidFlowFreePaths(7, 6, result.solutionPaths!!))
    }

    @Test
    fun `an unsolvable puzzle (endpoints that cannot cover the board) reports not solved`() {
        val result = solveFlowFree(size = 3, pairCount = 2, starts = listOf(0 to 0, 0 to 2), targets = listOf(0 to 0, 0 to 2))
        assertTrue(!result.solved)
    }
}
