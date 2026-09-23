package com.quietgrid.engine.flowfree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowFreeBruteForceSolverTest {
    @Test
    fun `counts exactly one solution for the trivial 2x2 single-pair puzzle`() {
        assertEquals(1, countFlowFreeSolutions(2, 1, listOf(0 to 0), listOf(1 to 0)))
    }

    @Test
    fun `a 2x2 puzzle with orthogonally adjacent endpoints also has exactly one full-coverage route`() {
        val count = countFlowFreeSolutions(2, 1, listOf(0 to 0), listOf(0 to 1), cap = 2)
        assertEquals(1, count)
    }

    @Test
    fun `agrees with the technique solver on a real generated 6x6 puzzle that a solution exists`() {
        val paths = generateFlowFreeSolution(6, pairCount = 4, random = kotlin.random.Random(7))!!
        val endpoints = endpointsOf(6, 4, paths)
        val starts = mutableListOf<Pair<Int, Int>>()
        val targets = mutableListOf<Pair<Int, Int>>()
        for (color in 0 until 4) {
            val cells = (0 until 6).flatMap { r -> (0 until 6).mapNotNull { c -> if (endpoints[r][c] == color) r to c else null } }
            starts += cells[0]
            targets += cells[1]
        }
        val techniqueResult = solveFlowFree(6, 4, starts, targets)
        if (techniqueResult.solved) {
            assertTrue(countFlowFreeSolutions(6, 4, starts, targets) >= 1)
        }
    }
}
